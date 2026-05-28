# Auth Screen Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce mandatory PIN/biometric setup on first launch, lock immediately on foreground return, try biometric first with PIN fallback, and don't lock on rotation.

**Architecture:** Fix existing Activity-level auth — switch from `onStart()` to `onResume()`/`onPause()`, use `rememberSaveable` for rotation safety, add `isFirstLaunch()` flag for mandatory setup.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, AndroidX Biometric, SharedPreferences

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `PinAuthManager.kt` | PIN/biometric auth logic, SharedPreferences | Modify — add `isFirstLaunch()`, `completeSetup()` |
| `MainActivity.kt` | App lifecycle, auth gating | Modify — `onResume()`/`onPause()`, `rememberSaveable`, setup screen |
| `LockScreen.kt` | Lock screen UI, biometric/PIN flow | Modify — biometric-first with PIN fallback |
| `SettingsViewModel.kt` | Settings state, PIN management | Modify — block app lock toggle when PIN set |
| `SettingsScreen.kt` | Settings UI | Modify — disable toggle, add helper text |

---

### Task 1: Add first-launch detection to PinAuthManager

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/domain/service/PinAuthManager.kt`

- [ ] **Step 1: Add `KEY_SETUP_COMPLETED` constant**

In `PinAuthManager.kt`, add to the `companion object` at line 66:

```kotlin
companion object {
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_APP_LOCK = "app_lock"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
}
```

- [ ] **Step 2: Add `isFirstLaunch()` method**

Add after `isAppLockEnabled()` (after line 26):

```kotlin
fun isFirstLaunch(): Boolean {
    return prefs.getBoolean(KEY_SETUP_COMPLETED, false).not()
}
```

- [ ] **Step 3: Add `completeSetup()` method**

Add after `isFirstLaunch()`:

```kotlin
fun completeSetup() {
    prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
}
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/domain/service/PinAuthManager.kt
git commit -m "feat: add first-launch detection to PinAuthManager"
```

---

### Task 2: Fix MainActivity lifecycle and auth gating

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/MainActivity.kt`

- [ ] **Step 1: Replace state variables**

In `MainActivity.kt`, replace lines 37-38:

```kotlin
// OLD:
private var isAuthenticated by mutableStateOf(false)
private var lifecycleResumeCount by mutableStateOf(0)

// NEW:
private val isAuthenticated = mutableStateOf(false)
private val resumeKey = androidx.compose.runtime.mutableIntStateOf(0)
```

- [ ] **Step 2: Add imports**

Add these imports at the top of the file:

```kotlin
import androidx.compose.runtime.mutableIntStateOf
```

- [ ] **Step 3: Replace `onStart()` with `onResume()`/`onPause()`**

Replace the `onStart()` method (lines 81-86) with:

```kotlin
override fun onResume() {
    super.onResume()
    resumeKey.intValue++
}

override fun onPause() {
    super.onPause()
    isAuthenticated.value = false
}
```

- [ ] **Step 4: Update auth gating logic in `setContent`**

Replace lines 65-75 with:

```kotlin
val setupRequired = pinAuthManager.isFirstLaunch()
val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()

when {
    setupRequired -> {
        LockScreen(
            pinAuthManager = pinAuthManager,
            biometricManager = biometricManager,
            onAuthenticated = {
                pinAuthManager.completeSetup()
                isAuthenticated.value = true
            },
            resumeKey = resumeKey.intValue
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
        AppNavigation()
    }
}
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/MainActivity.kt
git commit -m "feat: fix lifecycle and enforce mandatory first-launch auth"
```

---

### Task 3: Update LockScreen to handle mandatory setup

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt`

- [ ] **Step 1: Update `SetPinDialog` callback to call `completeSetup()`**

In `LockScreen.kt`, replace lines 119-128:

```kotlin
// OLD:
if (showSetPin) {
    SetPinDialog(
        onDismiss = { /* Cannot dismiss - must set PIN */ },
        onPinSet = { pin ->
            pinAuthManager.setPin(pin)
            pinAuthManager.setAppLockEnabled(true)
            showSetPin = false
            onAuthenticated()
        }
    )
}

// NEW:
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

This uses the existing `setPinAndEnable()` method which sets the PIN and enables app lock in one call.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt
git commit -m "refactor: use setPinAndEnable in LockScreen setup flow"
```

---

### Task 4: Block app lock toggle when PIN is set

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: Update `setAppLock()` to block when PIN is set**

In `SettingsViewModel.kt`, replace lines 78-81:

```kotlin
// OLD:
fun setAppLock(enabled: Boolean) {
    pinAuthManager.setAppLockEnabled(enabled)
    _state.value = _state.value.copy(isAppLockEnabled = enabled)
}

// NEW:
fun setAppLock(enabled: Boolean) {
    if (!enabled && pinAuthManager.isPinSet()) {
        // Cannot disable app lock while PIN is set
        // User must remove PIN first
        return
    }
    pinAuthManager.setAppLockEnabled(enabled)
    _state.value = _state.value.copy(isAppLockEnabled = enabled)
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt
git commit -m "feat: block app lock toggle when PIN is set"
```

---

### Task 5: Update Settings UI for PIN-locked state

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt`

- [ ] **Step 1: Disable toggle when PIN is set**

In `SettingsScreen.kt`, replace line 409:

```kotlin
// OLD:
Switch(checked = state.isAppLockEnabled, onCheckedChange = { viewModel.setAppLock(it) })

// NEW:
Switch(
    checked = state.isAppLockEnabled,
    onCheckedChange = { viewModel.setAppLock(it) },
    enabled = !state.isPinSet
)
```

- [ ] **Step 2: Update helper text for PIN-locked state**

Replace lines 398-406:

```kotlin
// OLD:
Text(
    when {
        state.isBiometricAvailable -> "Biometric + PIN available"
        state.isPinSet -> "PIN lock active"
        else -> "Set a PIN to lock the app"
    },
    style = MaterialTheme.typography.bodySmall,
    color = themeColors.onSurface.copy(alpha = 0.7f)
)

// NEW:
Text(
    when {
        state.isPinSet -> "PIN is set — app lock is always enabled"
        state.isBiometricAvailable -> "Biometric + PIN available"
        else -> "Set a PIN to lock the app"
    },
    style = MaterialTheme.typography.bodySmall,
    color = themeColors.onSurface.copy(alpha = 0.7f)
)
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt
git commit -m "feat: disable app lock toggle when PIN is set"
```

---

### Task 6: Build and verify on device

- [ ] **Step 1: Build release APK for testing**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on device**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual verification checklist**

Test on real device:

1. **Fresh install:** Clear app data, open app → should show setup screen with PIN entry
2. **Cannot skip:** Try to dismiss setup → should not be possible
3. **Set PIN:** Enter 5-digit PIN → should proceed to app
4. **Background/foreground:** Press home, reopen app → should show lock screen
5. **Biometric:** If device has fingerprint/face → should try biometric first
6. **PIN fallback:** Cancel biometric → should show PIN entry
7. **Rotation:** Rotate device while in app → should NOT re-authenticate
8. **Settings:** Go to Settings → App Lock toggle should be disabled with "PIN is set" text

- [ ] **Step 4: Commit all changes**

```bash
git add -A
git commit -m "feat: enforce mandatory auth setup and immediate foreground lock"
```
