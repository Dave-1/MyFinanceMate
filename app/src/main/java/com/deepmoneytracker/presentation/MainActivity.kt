package com.deepmoneytracker.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.presentation.navigation.AppNavigation
import com.deepmoneytracker.presentation.screens.LockScreen
import com.deepmoneytracker.presentation.theme.DeepMoneyTrackerTheme
import com.deepmoneytracker.presentation.theme.ThemeStateHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var themeStateHolder: ThemeStateHolder
    @Inject lateinit var pinAuthManager: PinAuthManager
    @Inject lateinit var biometricManager: BiometricManager

    private var isAuthenticated by mutableStateOf(false)
    private var lifecycleResumeCount by mutableStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRequiredPermissions()

        setContent {
            val currentTheme by themeStateHolder.currentTheme.collectAsState()
            val isDarkMode by themeStateHolder.isDarkMode.collectAsState()
            val isSystemTheme by themeStateHolder.isSystemTheme.collectAsState()

            val isDark = if (isSystemTheme) isSystemInDarkTheme() else isDarkMode

            DeepMoneyTrackerTheme(
                appTheme = currentTheme,
                darkTheme = isDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()
                    if (needsAuth && !isAuthenticated) {
                        LockScreen(
                            pinAuthManager = pinAuthManager,
                            biometricManager = biometricManager,
                            onAuthenticated = { isAuthenticated = true },
                            resumeKey = lifecycleResumeCount
                        )
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Reset auth every time Activity comes to foreground
        isAuthenticated = false
        lifecycleResumeCount++
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
