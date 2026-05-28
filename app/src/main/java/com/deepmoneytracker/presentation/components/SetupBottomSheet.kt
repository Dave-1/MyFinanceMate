package com.deepmoneytracker.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.presentation.theme.AppStrings
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import kotlinx.coroutines.launch

/**
 * Represents a single step in the setup flow.
 */
data class SetupStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String,
    val skipLabel: String? = null,
    val isCompleted: () -> Boolean,
    val onAction: () -> Unit,
    val onSkip: (() -> Unit)? = null
)

/**
 * Reusable multi-step bottom sheet for first-launch setup and other guided flows.
 *
 * Usage:
 * ```
 * SetupBottomSheet(
 *     title = "Welcome Setup",
 *     steps = listOf(
 *         SetupStep(icon = Icons.Default.Sms, title = "SMS Permission", ...)
 *     ),
 *     onDismiss = { ... }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupBottomSheet(
    title: String,
    steps: List<SetupStep>,
    onDismiss: () -> Unit,
    onAllComplete: () -> Unit = onDismiss
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val themeColors = LocalThemeColors.current
    var currentStep by remember { mutableIntStateOf(0) }

    // Check if all steps are already completed — use LaunchedEffect for side effect
    val allCompleted = steps.all { it.isCompleted() }
    LaunchedEffect(allCompleted) {
        if (allCompleted) {
            onAllComplete()
        }
    }
    if (allCompleted) return

    // Find first incomplete step
    val firstIncomplete = steps.indexOfFirst { !it.isCompleted() }
    if (firstIncomplete >= 0 && currentStep < firstIncomplete) {
        currentStep = firstIncomplete
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / steps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = themeColors.primary,
                trackColor = themeColors.primary.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Step content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step_content"
            ) { stepIndex ->
                if (stepIndex < steps.size) {
                    val step = steps[stepIndex]
                    StepContent(
                        step = step,
                        stepNumber = stepIndex + 1,
                        totalSteps = steps.size,
                        themeColors = themeColors,
                        onAction = {
                            step.onAction()
                            // Move to next incomplete step or complete
                            val nextIncomplete = steps.drop(stepIndex + 1).indexOfFirst { !it.isCompleted() }
                            if (nextIncomplete >= 0) {
                                currentStep = stepIndex + 1 + nextIncomplete
                            } else {
                                scope.launch {
                                    sheetState.hide()
                                    onAllComplete()
                                }
                            }
                        },
                        onSkip = step.onSkip?.let { skipAction ->
                            {
                                skipAction()
                                val nextIncomplete = steps.drop(stepIndex + 1).indexOfFirst { !it.isCompleted() }
                                if (nextIncomplete >= 0) {
                                    currentStep = stepIndex + 1 + nextIncomplete
                                } else {
                                    scope.launch {
                                        sheetState.hide()
                                        onAllComplete()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    step: SetupStep,
    stepNumber: Int,
    totalSteps: Int,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors,
    onAction: () -> Unit,
    onSkip: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step icon
        Card(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = themeColors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Step counter
        Text(
            text = "Step $stepNumber of $totalSteps",
            style = MaterialTheme.typography.labelMedium,
            color = themeColors.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = themeColors.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Action button
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(step.actionLabel, fontWeight = FontWeight.Medium)
        }

        // Skip button
        if (onSkip != null && step.skipLabel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text(step.skipLabel, color = themeColors.onBackground.copy(alpha = 0.6f))
            }
        }
    }
}

/**
 * Convenience composable for the first-launch welcome setup flow.
 */
@Composable
fun WelcomeSetupSheet(
    onDismiss: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onBackupSms: () -> Unit,
    smsPermissionGranted: Boolean,
    backupInProgress: Boolean,
    pinAuthManager: PinAuthManager,
    biometricManager: BiometricManager
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
        ?: throw IllegalStateException("WelcomeSetupSheet must be hosted in a FragmentActivity")
    var showSetPin by remember { mutableStateOf(false) }
    var pinSet by remember { mutableStateOf(pinAuthManager.isPinSet()) }
    var biometricEnabled by remember { mutableStateOf(false) }
    val biometricDialogTitle = stringResource(AppStrings.welcome_biometric_title)
    val biometricDialogSubtitle = stringResource(AppStrings.pin_biometric_subtitle)

    SetupBottomSheet(
        title = stringResource(AppStrings.welcome_title),
        steps = listOf(
            SetupStep(
                icon = Icons.Default.Sms,
                title = stringResource(AppStrings.welcome_sms_title),
                description = stringResource(AppStrings.welcome_sms_desc),
                actionLabel = if (smsPermissionGranted) stringResource(AppStrings.welcome_sms_granted) else stringResource(AppStrings.welcome_sms_grant),
                skipLabel = null,
                isCompleted = { smsPermissionGranted },
                onAction = onRequestSmsPermission
            ),
            SetupStep(
                icon = Icons.Default.Backup,
                title = stringResource(AppStrings.welcome_backup_title),
                description = stringResource(AppStrings.welcome_backup_desc),
                actionLabel = if (backupInProgress) stringResource(AppStrings.welcome_backup_in_progress) else stringResource(AppStrings.welcome_backup_now),
                skipLabel = stringResource(AppStrings.welcome_skip),
                isCompleted = { false },
                onAction = onBackupSms,
                onSkip = { /* skip backup */ }
            ),
            SetupStep(
                icon = Icons.Default.Lock,
                title = stringResource(AppStrings.welcome_pin_title),
                description = stringResource(AppStrings.welcome_pin_desc),
                actionLabel = if (pinSet) stringResource(AppStrings.welcome_pin_done) else stringResource(AppStrings.welcome_pin_set),
                skipLabel = null,
                isCompleted = { pinSet },
                onAction = { showSetPin = true }
            ),
            SetupStep(
                icon = Icons.Default.Fingerprint,
                title = stringResource(AppStrings.welcome_biometric_title),
                description = if (biometricManager.canAuthenticate())
                    stringResource(AppStrings.welcome_biometric_desc)
                else
                    stringResource(AppStrings.welcome_biometric_unavailable),
                actionLabel = if (biometricEnabled) stringResource(AppStrings.welcome_biometric_enabled) else stringResource(AppStrings.welcome_biometric_enable),
                skipLabel = stringResource(AppStrings.welcome_skip),
                isCompleted = { biometricEnabled || !biometricManager.canAuthenticate() },
                onAction = {
                    biometricManager.authenticate(
                        activity = activity,
                        title = biometricDialogTitle,
                        subtitle = biometricDialogSubtitle,
                        onSuccess = { biometricEnabled = true },
                        onError = { biometricEnabled = true },
                        onFailed = { biometricEnabled = true },
                    )
                },
                onSkip = { biometricEnabled = true }
            )
        ),
        onDismiss = {
            pinAuthManager.completeWelcome()
            onDismiss()
        }
    )

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onPinSet = { pin ->
                pinAuthManager.setPinAndEnable(pin)
                pinSet = true
                showSetPin = false
            }
        )
    }
}
