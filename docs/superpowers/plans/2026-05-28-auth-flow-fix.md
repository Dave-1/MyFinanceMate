# Auth Flow Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix auth flow bugs — welcome sheet is the only first-time setup, LockScreen is re-auth only, no visual overlap, no repeating sheets.

**Architecture:** WelcomeSheet handles first-time setup (SMS → Backup → PIN → Biometric). LockScreen handles re-auth only. MainActivity has three states: welcome not done, auth needed, app ready.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, AndroidX Biometric, SharedPreferences

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `PinAuthManager.kt` | Auth state, SharedPreferences | Modify — add `isWelcomeCompleted()`, `completeWelcome()`, remove `isFirstLaunch()` |
| `SetupBottomSheet.kt` | Welcome sheet UI | Modify — add PIN + biometric steps, add params |
| `MainActivity.kt` | Auth gating | Modify — three-state logic |
| `LockScreen.kt` | Re-auth screen | Modify — remove setup flow |
| `AppNavigation.kt` | Navigation + welcome sheet trigger | Modify — add params, trigger sheet |
| `DashboardViewModel.kt` | Dashboard state | Modify — check `isWelcomeCompleted()` |

---

### Task 1: Update PinAuthManager — add welcome completion tracking

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/domain/service/PinAuthManager.kt`

- [ ] **Step 1: Add `KEY_WELCOME_COMPLETED` constant**

In `PinAuthManager.kt`, add to `companion object` (after line 77):

```kotlin
companion object {
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_APP_LOCK = "app_lock"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_WELCOME_COMPLETED = "welcome_completed"
}
```

- [ ] **Step 2: Add `isWelcomeCompleted()` method**

Add after `completeSetup()` (after line 34):

```kotlin
fun isWelcomeCompleted(): Boolean = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)

fun completeWelcome() {
    prefs.edit().putBoolean(KEY_WELCOME_COMPLETED, true).apply()
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/domain/service/PinAuthManager.kt
git commit -m "feat: add welcome completion tracking to PinAuthManager"
```

---

### Task 2: Simplify LockScreen — remove setup flow, pure re-auth

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt`

- [ ] **Step 1: Remove `showSetPin` state and `SetPinDialog` block**

In `LockScreen.kt`, remove lines 53 and 119-128:

```kotlin
// REMOVE:
var showSetPin by remember { mutableStateOf(false) }

// REMOVE (lines 119-128):
if (showSetPin) {
    SetPinDialog(
        onDismiss = { /* Cannot dismiss - must set PIN */ },
        onPinSet = { pin ->
            pinAuthManager.setPinAndEnable(pin)
            showSetPin = false
            onAuthenticated()
        }
    )
}
```

- [ ] **Step 2: Remove `isPinSet` check in `LaunchedEffect`**

Replace lines 59-76:

```kotlin
// OLD:
LaunchedEffect(resumeKey, isPinSet) {
    if (isPinSet) {
        if (pinAuthManager.isBiometricAvailable()) {
            biometricManager.authenticate(
                activity = activity,
                title = "Unlock Deep Money Tracker",
                subtitle = "Verify your identity to continue",
                onSuccess = { onAuthenticated() },
                onError = { showVerifyPin = true },
                onFailed = { showVerifyPin = true }
            )
        } else {
            showVerifyPin = true
        }
    } else {
        showSetPin = true
    }
}

// NEW:
LaunchedEffect(resumeKey) {
    if (pinAuthManager.isBiometricAvailable()) {
        biometricManager.authenticate(
            activity = activity,
            title = "Unlock Deep Money Tracker",
            subtitle = "Verify your identity to continue",
            onSuccess = { onAuthenticated() },
            onError = { showVerifyPin = true },
            onFailed = { showVerifyPin = true }
        )
    } else {
        showVerifyPin = true
    }
}
```

- [ ] **Step 3: Remove `isPinSet` variable and simplify text**

Replace lines 56 and 103-114:

```kotlin
// REMOVE:
val isPinSet = pinAuthManager.isPinSet()

// REPLACE text (lines 103-114):
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
```

- [ ] **Step 4: Remove unused imports**

Remove these imports:
```kotlin
import com.deepmoneytracker.presentation.components.SetPinDialog
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt
git commit -m "refactor: simplify LockScreen to pure re-auth"
```

---

### Task 3: Update WelcomeSetupSheet — add PIN + biometric steps

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/components/SetupBottomSheet.kt`

- [ ] **Step 1: Add imports**

Add these imports at the top:

```kotlin
import androidx.compose.foundation.layout.width
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.presentation.components.SetPinDialog
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
```

- [ ] **Step 2: Add parameters to `WelcomeSetupSheet`**

Replace lines 280-287:

```kotlin
// OLD:
@Composable
fun WelcomeSetupSheet(
    onDismiss: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onBackupSms: () -> Unit,
    onSetupPin: () -> Unit,
    smsPermissionGranted: Boolean,
    backupInProgress: Boolean
)

// NEW:
@Composable
fun WelcomeSetupSheet(
    onDismiss: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onBackupSms: () -> Unit,
    smsPermissionGranted: Boolean,
    backupInProgress: Boolean,
    pinAuthManager: PinAuthManager,
    biometricManager: BiometricManager
)
```

- [ ] **Step 3: Replace the steps list**

Replace lines 288-322:

```kotlin
// OLD:
SetupBottomSheet(
    title = "Welcome to Deep Money Tracker",
    steps = listOf(
        SetupStep(...),  // SMS
        SetupStep(...),  // Backup
        SetupStep(...)   // PIN (with onSetupPin callback)
    ),
    onDismiss = onDismiss
)

// NEW:
val context = LocalContext.current
val activity = context as FragmentActivity
var showSetPin by remember { mutableStateOf(false) }
var pinSet by remember { mutableStateOf(false) }
var biometricEnabled by remember { mutableStateOf(false) }

SetupBottomSheet(
    title = "Welcome to Deep Money Tracker",
    steps = listOf(
        SetupStep(
            icon = Icons.Default.Sms,
            title = "SMS Access",
            description = "Allow the app to read your SMS messages to automatically track bank transactions.",
            actionLabel = if (smsPermissionGranted) "Permission Granted" else "Grant Permission",
            skipLabel = null,
            isCompleted = { smsPermissionGranted },
            onAction = onRequestSmsPermission
        ),
        SetupStep(
            icon = Icons.Default.Backup,
            title = "Backup SMS",
            description = "Back up and parse your SMS messages to populate transaction data.",
            actionLabel = if (backupInProgress) "Backing up..." else "Backup Now",
            skipLabel = "Skip for now",
            isCompleted = { false },
            onAction = onBackupSms,
            onSkip = { /* skip backup */ }
        ),
        SetupStep(
            icon = Icons.Default.Lock,
            title = "Set PIN",
            description = "Set up a 5-digit PIN to protect your financial data.",
            actionLabel = if (pinSet) "PIN Set" else "Set PIN",
            skipLabel = null,
            isCompleted = { pinSet },
            onAction = { showSetPin = true }
        ),
        SetupStep(
            icon = Icons.Default.Fingerprint,
            title = "Enable Biometric",
            description = if (biometricManager.isBiometricAvailable())
                "Use fingerprint or face recognition for quick unlock."
            else
                "Biometric not available on this device.",
            actionLabel = if (biometricEnabled) "Enabled" else "Enable Biometric",
            skipLabel = "Skip for now",
            isCompleted = { biometricEnabled || !biometricManager.isBiometricAvailable() },
            onAction = {
                biometricManager.authenticate(
                    activity = activity,
                    title = "Enable Biometric",
                    subtitle = "Verify to enable biometric unlock",
                    onSuccess = { biometricEnabled = true },
                    onError = { /* skip */ },
                    onFailed = { /* skip */ }
                )
            },
            onSkip = { biometricEnabled = true } // Mark as completed on skip
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
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/components/SetupBottomSheet.kt
git commit -m "feat: add PIN and biometric steps to WelcomeSetupSheet"
```

---

### Task 4: Update AppNavigation — accept params and trigger welcome sheet

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt`

- [ ] **Step 1: Add imports**

Add these imports:

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.presentation.components.WelcomeSetupSheet
```

- [ ] **Step 2: Add parameters to `AppNavigation`**

Replace line 59:

```kotlin
// OLD:
@Composable
fun AppNavigation()

// NEW:
@Composable
fun AppNavigation(
    showWelcomeSheet: Boolean = false,
    pinAuthManager: PinAuthManager? = null,
    biometricManager: BiometricManager? = null
)
```

- [ ] **Step 3: Add welcome sheet trigger logic**

Add after line 63 (after `val themeColors = LocalThemeColors.current`):

```kotlin
var welcomeSheetShown by remember { mutableStateOf(false) }

if (showWelcomeSheet && !welcomeSheetShown && pinAuthManager != null && biometricManager != null) {
    WelcomeSetupSheet(
        onDismiss = { welcomeSheetShown = true },
        onRequestSmsPermission = { /* handled by DashboardScreen */ },
        onBackupSms = { /* handled by DashboardScreen */ },
        smsPermissionGranted = false,
        backupInProgress = false,
        pinAuthManager = pinAuthManager,
        biometricManager = biometricManager
    )
}
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt
git commit -m "feat: add welcome sheet support to AppNavigation"
```

---

### Task 5: Update MainActivity — three-state auth logic

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/MainActivity.kt`

- [ ] **Step 1: Replace the `when` block**

Replace lines 66-92:

```kotlin
// OLD:
val setupRequired = pinAuthManager.isFirstLaunch()
val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()

when {
    setupRequired -> {
        LockScreen(...)
    }
    needsAuth && !isAuthenticated.value -> {
        LockScreen(...)
    }
    else -> {
        AppNavigation()
    }
}

// NEW:
val welcomeCompleted = pinAuthManager.isWelcomeCompleted()
val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()

when {
    !welcomeCompleted -> {
        AppNavigation(
            showWelcomeSheet = true,
            pinAuthManager = pinAuthManager,
            biometricManager = biometricManager
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
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/MainActivity.kt
git commit -m "feat: implement three-state auth logic in MainActivity"
```

---

### Task 6: Update DashboardViewModel — check welcome completion

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/DashboardViewModel.kt`

- [ ] **Step 1: Add PinAuthManager injection**

Replace constructor (lines 39-43):

```kotlin
// OLD:
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val smsBackupParser: SmsBackupParser
)

// NEW:
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val smsBackupParser: SmsBackupParser,
    private val pinAuthManager: PinAuthManager
)
```

- [ ] **Step 2: Add import**

```kotlin
import com.deepmoneytracker.domain.service.PinAuthManager
```

- [ ] **Step 3: Modify `_showBackupReminder` initialization**

Replace lines 46-48:

```kotlin
// OLD:
private val _showBackupReminder = MutableStateFlow(
    smsBackupParser.getLastBackupTimestamp() == 0L || smsBackupParser.isBackupNeeded()
)

// NEW:
private val _showBackupReminder = MutableStateFlow(
    !pinAuthManager.isWelcomeCompleted() && (
        smsBackupParser.getLastBackupTimestamp() == 0L || smsBackupParser.isBackupNeeded()
    )
)
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/DashboardViewModel.kt
git commit -m "fix: don't show backup reminder after welcome completed"
```

---

### Task 7: Build and verify

- [ ] **Step 1: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on device**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual verification checklist**

1. **Fresh install:** WelcomeSheet shows → SMS → Backup → Set PIN → Enable Biometric → Dashboard
2. **Welcome doesn't repeat:** Close app, reopen → LockScreen (not WelcomeSheet)
3. **Re-auth works:** Background/foreground → LockScreen with biometric first, PIN fallback
4. **No visual overlap:** LockScreen only shows lock icon + biometric/PIN dialog (no setup text)
5. **Biometric requested:** During welcome, biometric prompt appears if device supports it
