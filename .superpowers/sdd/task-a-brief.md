# Task A: Bug Fixes

Fix these 5 bugs in MyFinanceMate Android app (Kotlin, Jetpack Compose, Room, Hilt).
Working dir: C:\Users\Navnath\DailyWork\projects\projects-using-opencode\MyFinanceMate

## Bug 1 — SmsParser ignores actual SMS date
**File:** `app/src/main/java/com/myfinancemate/domain/service/SmsParser.kt`
`parse(smsBody, senderId)` currently returns `ParsedTransaction(date = System.currentTimeMillis())`.
This makes all SMS-imported transactions get today's date instead of the real SMS date.
**Fix:** Add `date: Long` parameter to `parse()`. Signature: `parse(smsBody: String, senderId: String, date: Long): ParsedTransaction?`. Use it for `ParsedTransaction.date`.

**File:** `app/src/main/java/com/myfinancemate/domain/service/SmsBackupParser.kt`
In `backupAndParse()` at the bank-SMS parse call, pass `date = sms.date`:
`val parsed = smsParser.parse(sms.body, sms.address, sms.date)`

## Bug 2 — EditTransactionScreen truncates decimals
**File:** `app/src/main/java/com/myfinancemate/presentation/screens/EditTransactionScreen.kt` line 64
Current: `amount = transaction.amount.toLong().toString()` — turns 2435.38 into 2435.
**Fix:** `amount = transaction.amount.toBigDecimal().stripTrailingZeros().toPlainString()`

## Bug 3 — ReportsScreen PieChart white donut hole
**File:** `app/src/main/java/com/myfinancemate/presentation/screens/ReportsScreen.kt` line 310
`drawCircle(color = Color.White, ...)` breaks dark mode.
**Fix:** Use `themeColors.background` instead of `Color.White`. `themeColors` is available in the composable scope.

## Bug 4 — LockScreen inconsistent theming
**File:** `app/src/main/java/com/myfinancemate/presentation/screens/LockScreen.kt`
Uses `MaterialTheme.colorScheme.*` (background, primaryContainer, onPrimaryContainer, onBackground) while every other screen uses `LocalThemeColors`.
**Fix:** Replace all `MaterialTheme.colorScheme.X` with `themeColors.X` where mapping exists (background→background, primaryContainer→primaryContainer, onPrimaryContainer→onPrimaryContainer, onBackground→onBackground). Add `val themeColors = LocalThemeColors.current` at top of `LockScreen()` composable.

## Bug 5 — DashboardViewModel silently swallows exceptions
**File:** `app/src/main/java/com/myfinancemate/presentation/viewmodel/DashboardViewModel.kt` line 66
`catch (_: Exception) {}` hides backup failures.
**Fix:** Log the exception and expose an error in state. Add `val backupError: String? = null` to `DashboardState`. In `backupSms()`, set `backupError = e.message` on catch, clear on start and on success. Use `android.util.Log.e("DashboardViewModel", "Backup failed", e)`.

## Report
Write full report to `.superpowers/sdd/task-a-report.md`. Return: status, commits, one-line test summary, concerns.
