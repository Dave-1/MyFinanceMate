# SMS Utility App Refinement — Design Spec

**Date:** 2026-05-27
**Status:** Approved
**Approach:** Phased implementation (4 phases)

---

## Overview

Refine the DeepMoneyTracker SMS utility app with bug fixes, backup/parsing improvements, notifications overhaul, and UI polish. All changes follow Material 3 design guidelines.

---

## Phase 1: Bug Fixes

### 1a. Transactions Label Wrapping
**Problem:** "Transactions" label in bottom nav wraps to two lines on small screens.
**Fix:** Add `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, and `softWrap = false` to the `NavigationBarItem` label. Consider using `fontSize = 11.sp` for bottom nav labels to prevent wrapping with 5 tabs.

**Files:** `AppNavigation.kt`

### 1b. SMS Chip Filtering
**Problem:** Notification category chips show counts but don't filter data on click. Data disappears after clicking other chips and doesn't reload on app reopen.
**Fix:**
- Verify `NotificationsViewModel.setCategory()` correctly updates `selectedCategory` MutableStateFlow
- Ensure the `combine` operator re-emits when `selectedCategory` changes
- Add `LaunchedEffect` in NotificationsPage to reset filter on initial load
- Ensure `SharingStarted.WhileSubscribed(5000)` doesn't cause stale data

**Files:** `NotificationsViewModel.kt`, `NotificationsPage.kt`

### 1c. Rotation Handling
**Problem:** UI breaks on screen rotation. `selectedFilter` uses `remember` which loses state on config change.
**Fix:**
- Replace `remember { mutableStateOf(...) }` with `rememberSaveable { mutableStateOf(...) }` for filter state in TransactionsScreen
- Same for any other screen-level state that should survive rotation
- Test with `configChanges` or proper state restoration

**Files:** `TransactionsScreen.kt`, `NotificationsPage.kt`

### 1d. PIN Modal Auto-Close
**Problem:** VerifyPinDialog doesn't auto-close when PIN is correct. Shows error on incorrect but stays open even on correct.
**Fix:** The `VerifyPinDialog` already calls `onVerified()` on success. The caller must dismiss the dialog by toggling visibility state. Ensure the caller's `onVerified` callback sets `showVerifyPin = false`.

**Files:** `PinDialog.kt`, `LockScreen.kt`, `SettingsScreen.kt`

---

## Phase 2: SMS Backup & Parsing

### 2a. One-Click SMS Backup
**Design:** Add "Backup SMS" button in Settings → SMS Utilities section. Uses `SmsBackupParser.backupAndParse()` which:
1. Reads all SMS via `content://sms` content provider
2. Writes XML backup to `sms_backups/` directory
3. Parses SMS: bank transactions → `TransactionEntity`, non-bank → `SmsNotificationEntity`
4. Shows progress indicator during backup
5. Displays result summary (total SMS, bank transactions, notifications created, bank breakdown)

**Files:** `SettingsScreen.kt`, `SettingsViewModel.kt`, `SmsBackupParser.kt`

### 2b. View Backup Files
**Design:** Add "View Backups" option in Settings that lists files from `sms_backups/` directory. Each entry shows filename, date, size. Tap to share via FileProvider. Long-press to delete.

**Files:** `SettingsScreen.kt`, `SettingsViewModel.kt`, `FileProvider` config

### 2c. Auto-Parse on Backup
**Design:** `SmsBackupParser.backupAndParse()` already handles this. Bank SMS parsed by `SmsParser` + `CategorizationEngine` → `TransactionEntity`. Non-bank SMS classified by `SmsNotificationClassifier` → `SmsNotificationEntity` with categories: RECHARGE, EXPIRY, PROMOTION, OTP, DELIVERY, APPOINTMENT, OTHER.

**Duplicate Prevention:** `transactionRepository.existsByBodyAndDate()` and `smsNotificationRepository.existsByBody()` checked before insert.

**Files:** `SmsBackupParser.kt`, `SmsNotificationClassifier.kt`

### 2d. Category Tabs Under Transactions
**Design:** Extend filter chips in TransactionsScreen: "All | Income | Expense | SMS | [Bank names...]"
- "SMS" filter: `transactions.filter { it.isFromSms }`
- Per-bank sub-tabs: dynamically generated from `SmsRuleEntity.senderName` — each bank gets its own chip (e.g., "HDFC", "SBI", "ICICI"). Tapping a bank chip filters to that bank's transactions only.
- Bank chips shown in a horizontally scrollable `LazyRow` after the fixed chips (All/Income/Expense/SMS)

**Files:** `TransactionsScreen.kt`, `TransactionViewModel.kt`

### 2e. Backup Reminder
**Design:** Check `SmsBackupParser.getLastBackupTimestamp()` on app launch. If >2 days since last backup, show a gentle reminder card on Dashboard. No blocking prompts — just a dismissible card with "Backup Now" button.

**Files:** `DashboardScreen.kt`, `DashboardViewModel.kt`

---

## Phase 3: Notifications Overhaul

### 3a. Notifications Page Enhancement
**Design:** Existing NotificationsPage with category filter chips. Enhancements:
- Add unread count badge on bottom nav Notifications icon (already implemented)
- Show "No notifications" empty state with icon when filtered results are empty
- Expandable card body on tap (already implemented)

**Files:** `NotificationsPage.kt`

### 3b. Sorting Fix
**Design:** Current: `(expired + regular).sortedByDescending { it.smsDate }`. Fix: sort by priority first, then date:
1. Unread + expired (pinned to top)
2. Unread + regular (next)
3. Read items (bottom, sorted by date descending)

**Files:** `NotificationsPage.kt`, `NotificationsViewModel.kt`

### 3c. Offline Notifications
**Design:** `SmsNotificationWorker` already scheduled via WorkManager. Enhance:
- Run every 6 hours
- Check for expired unread notifications → show Android system notification
- Check for upcoming RECHARGE/EXPIRY within 24h → show reminder notification
- Notification tap opens NotificationsPage

**Files:** `SmsNotificationWorker.kt`, `AppModule.kt` (WorkManager scheduling)

### 3d. Expired Reminders Migration
**Design:** `SmsNotificationClassifier.isExpiredNotification()` marks SMS as expired if they contain expiry/recharge keywords and are >1 day old. The worker should also check `ReminderEntity` for overdue reminders and create corresponding `SmsNotificationEntity` entries with `isExpired = true`.

**Files:** `SmsNotificationWorker.kt`, `ReminderRepository.kt`

---

## Phase 4: UI Polish

### 4a. Date-Sorted Accordions
**Design:** All SMS views use `DateAccordionList` component. Verify:
- TransactionsScreen: ✓ (already uses it)
- NotificationsPage: ✓ (already uses it)
- Dashboard recent transactions: add date accordions (currently flat list)
- Reports: group by date if showing transaction lists

**Files:** `DashboardScreen.kt`, `ReportsScreen.kt`

### 4b. Swipe Actions
**Design:** Swipe gestures on notification cards (already implemented in NotificationsPage):
- Right swipe (StartToEnd) → Mark as Read
- Left swipe (EndToStart) → Delete
- Apply same pattern to TransactionsScreen cards

**Files:** `TransactionsScreen.kt`, `NotificationsPage.kt`

### 4c. Material 3 Compliance
**Design:** Audit all screens for M3 compliance:
- Use `MaterialTheme.colorScheme` consistently (already using custom `ThemeColors`)
- All cards use `CardDefaults.cardColors()`
- Filter chips use `FilterChipDefaults.filterChipColors()`
- TopAppBar uses `TopAppBarDefaults.topAppBarColors()`
- Ensure consistent corner radius (14.dp for cards, 20.dp for chips)

**Files:** All screen files

### 4d. Bottom Navigation Tabs
**Design:** Current 5 tabs (Home, Transactions, Reminders, Notifications, Settings). Keep as-is. Add child filter chips inside Transactions screen for category filtering (already has All/Income/Expense — extend with SMS/By Bank from Phase 2d).

**Files:** `AppNavigation.kt`

---

## Implementation Order

1. Phase 1 (Bug Fixes) — 4 items, estimated 1-2 hours
2. Phase 2 (Backup & Parsing) — 5 items, estimated 2-3 hours
3. Phase 3 (Notifications) — 4 items, estimated 2-3 hours
4. Phase 4 (UI Polish) — 4 items, estimated 1-2 hours

**Total:** 17 items across 4 phases

---

## Files Modified (Summary)

| Phase | Files |
|-------|-------|
| 1 | `AppNavigation.kt`, `NotificationsViewModel.kt`, `NotificationsPage.kt`, `TransactionsScreen.kt`, `PinDialog.kt`, `LockScreen.kt` |
| 2 | `SettingsScreen.kt`, `SettingsViewModel.kt`, `SmsBackupParser.kt`, `TransactionsScreen.kt`, `TransactionViewModel.kt`, `DashboardScreen.kt` |
| 3 | `NotificationsPage.kt`, `NotificationsViewModel.kt`, `SmsNotificationWorker.kt`, `AppModule.kt`, `ReminderRepository.kt` |
| 4 | `DashboardScreen.kt`, `ReportsScreen.kt`, `TransactionsScreen.kt`, `NotificationsPage.kt`, `AppNavigation.kt` |
