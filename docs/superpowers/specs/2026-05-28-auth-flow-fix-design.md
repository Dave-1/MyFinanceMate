# Auth Flow Fix — Design Spec

## Problem

The current auth system has multiple bugs:
1. On fresh install, LockScreen shows PIN setup + biometric + "App locked" text simultaneously (visual overlap)
2. Welcome sheet repeats on every launch because dismissal is ViewModel-scoped (not persisted)
3. Biometric is never triggered on first launch (PIN check happens first, biometric path unreachable)
4. LockScreen mixes setup and re-auth concerns in one composable

## Requirements

1. **Welcome sheet is the only first-time setup** — replaces LockScreen for setup
2. **Persist welcome completion** — never show welcome sheet again after completion
3. **Biometric enrollment** — request biometric during welcome sheet setup
4. **LockScreen is re-auth only** — no setup logic, only biometric/PIN verify
5. **Clean flow:** Install → WelcomeSheet → Dashboard → (next launch) LockScreen → Dashboard

## Approach

WelcomeSheet handles first-time setup (SMS → Backup → PIN → Biometric). LockScreen handles re-auth only. MainActivity has three states: welcome not done, auth needed, app ready.

## Changes

### 1. PinAuthManager.kt

**Add:**
```kotlin
private const val KEY_WELCOME_COMPLETED = "welcome_completed"

fun isWelcomeCompleted(): Boolean = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)

fun completeWelcome() {
    prefs.edit().putBoolean(KEY_WELCOME_COMPLETED, true).apply()
}
```

**Remove:** `isFirstLaunch()` and `completeSetup()` — replaced by `isWelcomeCompleted()` / `completeWelcome()`.

**No change to `setPinAndEnable()`** — WelcomeSheet calls `completeWelcome()` explicitly after PIN is set.

### 2. SetupBottomSheet.kt (WelcomeSetupSheet)

**Current flow (3 steps):** SMS → Backup → PIN setup

**New flow (4 steps):** SMS → Backup → Set PIN → Enable Biometric

**Parameters to add:** `pinAuthManager: PinAuthManager`, `biometricManager: BiometricManager`

**Step 3 (Set PIN):**
- Show `SetPinDialog` inline
- On PIN set: call `pinAuthManager.setPinAndEnable(pin)`

**Step 4 (Enable Biometric):**
- Check `biometricManager.canAuthenticate()`
- If available: show "Enable Biometric" button → launch `BiometricPrompt` via `biometricManager.authenticate()`
- If not available: show "Biometric not available" message
- "Skip" button always available

**Completion:**
- Call `pinAuthManager.completeWelcome()`
- Dismiss sheet
- App is unlocked

**`WelcomeSetupSheet` wrapper function:** Update to pass `pinAuthManager` and `biometricManager` to `SetupBottomSheet`.

### 3. MainActivity.kt

**Replace `when` block:**
```kotlin
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

### 4. LockScreen.kt

**Remove:**
- `showSetPin` state and `SetPinDialog` block
- `isPinSet` check in `LaunchedEffect`

**Keep:**
- Biometric-first flow
- Lock icon + "App Locked" text
- `LaunchedEffect(resumeKey)` for re-auth

### 5. AppNavigation.kt

**Add parameter:** `showWelcomeSheet: Boolean = false`

**Add parameters to `AppNavigation`:** `pinAuthManager: PinAuthManager`, `biometricManager: BiometricManager`

**Add logic:** If `showWelcomeSheet` is true, trigger `WelcomeSetupSheet` on first composition using a `remember` + `LaunchedEffect` pattern:
```kotlin
var welcomeSheetShown by remember { mutableStateOf(false) }

if (showWelcomeSheet && !welcomeSheetShown) {
    // Show WelcomeSetupSheet
    WelcomeSetupSheet(
        pinAuthManager = pinAuthManager,
        biometricManager = biometricManager,
        onDismiss = { welcomeSheetShown = true }
    )
}
```

### 6. DashboardViewModel.kt

**Modify `showBackupReminder`:** Also check `pinAuthManager.isWelcomeCompleted()` — if welcome is completed, don't show backup reminder as welcome sheet.

## Files Modified

| File | Changes |
|---|---|
| `PinAuthManager.kt` | Add `isWelcomeCompleted()`, `completeWelcome()`, remove `isFirstLaunch()` |
| `SetupBottomSheet.kt` | Add PIN + biometric steps, call `completeWelcome()` |
| `MainActivity.kt` | Three-state logic, pass `showWelcomeSheet` |
| `LockScreen.kt` | Remove setup flow, pure re-auth |
| `AppNavigation.kt` | Accept `showWelcomeSheet`, `pinAuthManager`, `biometricManager` params |
| `DashboardViewModel.kt` | Check `isWelcomeCompleted()` for backup reminder |

## Verification

1. **Fresh install:** WelcomeSheet shows → SMS → Backup → Set PIN → Enable Biometric → Dashboard
2. **Welcome doesn't repeat:** Close app, reopen → LockScreen (not WelcomeSheet)
3. **Re-auth works:** Background/foreground → LockScreen with biometric first, PIN fallback
4. **No visual overlap:** LockScreen only shows lock icon + biometric/PIN dialog (no setup text)
5. **Biometric requested:** During welcome, biometric prompt appears if device supports it
