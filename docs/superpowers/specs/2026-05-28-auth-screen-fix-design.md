# Auth Screen Fix — Design Spec

## Problem

The current auth system has gaps:
1. PIN/biometric setup is optional on first launch — users can skip and never enable security
2. Auth only triggers when BOTH `isPinSet()` AND `isAppLockEnabled()` are true
3. Auth resets on screen rotation (because `onStart()` fires during config changes)
4. No enforced security for a finance tracker app

## Requirements

1. **Mandatory first-launch setup:** User must set PIN before using the app
2. **Immediate foreground lock:** Re-authenticate immediately when app returns from background
3. **Biometric first:** Try biometric auth first, fall back to PIN if unavailable/failed
4. **Rotation-safe:** Don't re-authenticate on screen rotation

## Approach

Fix the existing Activity-level auth (no new dependencies). Use `onResume()`/`onPause()` instead of `onStart()`, and `rememberSaveable` for rotation safety.

## Changes

### 1. PinAuthManager.kt

Add two methods:

```kotlin
fun isFirstLaunch(): Boolean {
    return prefs.getBoolean("setup_completed", false).not()
}

fun completeSetup() {
    prefs.edit().putBoolean("setup_completed", true).apply()
}
```

- `isFirstLaunch()` returns `true` when `setup_completed` is false (default)
- `completeSetup()` marks setup as done after user sets PIN

The existing `isPinSet()` check is sufficient — if no PIN is set, `isFirstLaunch()` is true.

### 2. MainActivity.kt

**State changes (class-level properties):**
```kotlin
// Replace:
private var isAuthenticated by mutableStateOf(false)
private var lifecycleResumeCount by mutableStateOf(0)

// With:
private var isAuthenticated = mutableStateOf(false)
private var resumeKey = mutableIntStateOf(0)
```

These must be class-level (not inside `setContent`) so `onPause()` can access them.

**Lifecycle changes:**
```kotlin
// Replace onStart() with:
override fun onResume() {
    super.onResume()
    resumeKey.intValue++
}

override fun onPause() {
    super.onPause()
    isAuthenticated.value = false
}
```

**Auth gating logic (inside setContent):**
```kotlin
val setupRequired = pinAuthManager.isFirstLaunch()
val needsAuth = pinAuthManager.isPinSet() && pinAuthManager.isAppLockEnabled()

when {
    setupRequired -> {
        // Mandatory setup — reuse SetPinDialog in full-screen mode
        SetPinDialog(
            onPinSet = {
                pinAuthManager.setPinAndEnable(it)
                pinAuthManager.completeSetup()
                isAuthenticated.value = true
            },
            onDismiss = { /* cannot dismiss — mandatory */ }
        )
    }
    needsAuth && !isAuthenticated.value -> {
        LockScreen(
            pinAuthManager = pinAuthManager,
            biometricManager = biometricManager,
            activity = this,
            onAuthenticated = { isAuthenticated.value = true },
            resumeKey = resumeKey.intValue
        )
    }
    else -> {
        AppNavigation()
    }
}
```

Note: `activity = this` passes the Activity reference to `LockScreen` so it can launch `BiometricPrompt` (which requires `FragmentActivity`).

### 3. LockScreen.kt

Add `activity` parameter and try biometric first:

```kotlin
@Composable
fun LockScreen(
    pinAuthManager: PinAuthManager,
    biometricManager: BiometricManager,
    activity: FragmentActivity,
    onAuthenticated: () -> Unit,
    resumeKey: Int
) {
    var showPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resumeKey) {
        if (biometricManager.canAuthenticate()) {
            biometricManager.authenticate(
                activity = activity,
                title = "Unlock DeepMoneyTracker",
                subtitle = "Verify your identity",
                onSuccess = { onAuthenticated() },
                onError = { showPinDialog = true },
                onFailed = { showPinDialog = true }
            )
        } else {
            showPinDialog = true
        }
    }

    // UI: lock icon, "App Locked" text
    // VerifyPinDialog when showPinDialog = true
}
```

**Key change:** Biometric is attempted first via `biometricManager.authenticate()`. If `canAuthenticate()` returns false or biometric fails, fall back to `VerifyPinDialog`.

### 4. SettingsScreen.kt / SettingsViewModel.kt

**SettingsViewModel.kt:**
```kotlin
fun setAppLock(enabled: Boolean) {
    if (!enabled && pinAuthManager.isPinSet()) {
        // Cannot disable app lock while PIN is set
        // Option A: Clear PIN first
        // Option B: Block toggle and show message
        return
    }
    pinAuthManager.setAppLock(enabled)
}
```

**SettingsScreen.kt:**
- If PIN is set, the App Lock toggle is always ON and disabled
- Show helper text: "PIN is set — app lock is always enabled"
- To disable, user must first remove PIN via "Remove PIN" button

## Files Modified

| File | Changes |
|---|---|
| `PinAuthManager.kt` | Add `isFirstLaunch()`, `completeSetup()` |
| `MainActivity.kt` | `onResume()`/`onPause()`, `rememberSaveable`, setup gating |
| `LockScreen.kt` | Biometric-first flow, improved fallback logic |
| `SettingsViewModel.kt` | Toggle behavior when PIN is set |
| `SettingsScreen.kt` | Updated toggle UI/helper text |

## Verification

1. **Fresh install:** App shows setup screen, cannot proceed without setting PIN
2. **Background/foreground:** App locks immediately when returning from background
3. **Biometric available:** Tries biometric first, falls back to PIN on failure
4. **Biometric unavailable:** Shows PIN entry directly
5. **Screen rotation:** Stays authenticated, no re-auth triggered
6. **Settings:** Cannot disable app lock while PIN is set
