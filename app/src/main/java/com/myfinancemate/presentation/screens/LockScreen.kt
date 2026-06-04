package com.myfinancemate.presentation.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myfinancemate.R
import com.myfinancemate.presentation.theme.AppStrings
import androidx.fragment.app.FragmentActivity
import com.myfinancemate.domain.service.BiometricManager
import com.myfinancemate.domain.service.PinAuthManager

import com.myfinancemate.presentation.components.VerifyPinDialog

@Composable
fun LockScreen(
    pinAuthManager: PinAuthManager,
    biometricManager: BiometricManager,
    onAuthenticated: () -> Unit,
    resumeKey: Int = 0
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var showVerifyPin by remember { mutableStateOf(false) }
    val appName = stringResource(com.myfinancemate.R.string.app_name)

    // Auto-trigger biometric/PIN on first composition and every resume
    LaunchedEffect(resumeKey) {
        if (pinAuthManager.isBiometricAvailable()) {
            biometricManager.authenticate(
                activity = activity,
                title = appName,
                subtitle = "Verify your identity to continue",
                onSuccess = { onAuthenticated() },
                onError = { showVerifyPin = true },
                onFailed = { showVerifyPin = true }
            )
        } else {
            showVerifyPin = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(AppStrings.pin_locked),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(AppStrings.pin_locked_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }

    if (showVerifyPin) {
        VerifyPinDialog(
            onDismiss = { /* Cannot dismiss - must verify */ },
            onVerified = {
                showVerifyPin = false
                onAuthenticated()
            },
            onVerify = { pin -> pinAuthManager.verifyPin(pin) }
        )
    }
}
