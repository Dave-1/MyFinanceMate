package com.myfinancemate.presentation

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.myfinancemate.domain.service.BiometricManager
import com.myfinancemate.domain.service.PinAuthManager
import com.myfinancemate.presentation.navigation.AppNavigation
import com.myfinancemate.presentation.screens.LockScreen
import com.myfinancemate.presentation.theme.MyFinanceMateTheme
import com.myfinancemate.presentation.theme.ThemeStateHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var themeStateHolder: ThemeStateHolder
    @Inject lateinit var pinAuthManager: PinAuthManager
    @Inject lateinit var biometricManager: BiometricManager
    @Inject lateinit var smsBackupParser: com.myfinancemate.domain.service.SmsBackupParser

    private val isAuthenticated = mutableStateOf(false)
    private val resumeKey = mutableIntStateOf(0)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isAuthenticated", isAuthenticated.value)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore auth state across Activity recreation (e.g., theme change)
        if (savedInstanceState != null) {
            isAuthenticated.value = savedInstanceState.getBoolean("isAuthenticated", false)
        }
        // Permission requests are now handled by the WelcomeSetupSheet on first launch
        // requestRequiredPermissions() — removed, handled by setup flow

        setContent {
            val currentTheme by themeStateHolder.currentTheme.collectAsState()
            val isDarkMode by themeStateHolder.isDarkMode.collectAsState()
            val isSystemTheme by themeStateHolder.isSystemTheme.collectAsState()

            val isDark = if (isSystemTheme) isSystemInDarkTheme() else isDarkMode

            MyFinanceMateTheme(
                appTheme = currentTheme,
                darkTheme = isDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val welcomeCompleted = pinAuthManager.isWelcomeCompleted()
                    val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()

                    when {
                        !welcomeCompleted -> {
                            AppNavigation(
                                showWelcomeSheet = true,
                                pinAuthManager = pinAuthManager,
                                biometricManager = biometricManager,
                                smsBackupParser = smsBackupParser
                            )
                        }
                        needsAuth && !isAuthenticated.value -> {
                            LockScreen(
                                pinAuthManager = pinAuthManager,
                                biometricManager = biometricManager,
                                onAuthenticated = { isAuthenticated.value = true },
                                resumeKey = resumeKey.intValue
                            )
                        }
                        else -> {
                            AppNavigation(
                                pinAuthManager = pinAuthManager,
                                biometricManager = biometricManager
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeKey.intValue++
    }

    override fun onPause() {
        super.onPause()
        // Do not clear auth state here to avoid losing it during configuration changes
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        isAuthenticated.value = false
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
