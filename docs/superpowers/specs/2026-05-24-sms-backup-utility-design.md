# SMS Backup & Parse Utility — Design Spec

## Context

The app currently requires being set as the default SMS app to receive real-time SMS broadcasts. On real devices, users don't want to change their default messaging app. The app also has too many SMS-related permissions (7 total), which causes Google Play rejections for apps that aren't primarily SMS utilities.

**Solution**: Position the app as a legitimate SMS utility by adding a one-click SMS backup feature. The app reads ALL SMS from the device, saves them to a local XML file (genuine backup), and parses bank SMS into financial transactions. This removes the need for default SMS app status while giving users real value.

**Approach**: Full Backup + Selective Parse (Approach A)
- User taps one button → app reads ALL SMS → saves complete XML backup → parses only bank SMS matching active rules → inserts transactions with actual SMS dates → shows result summary
- Zero manual steps from the user's perspective
- Dashboard reminder card for first-time users
- Auto-open backup modal after 2 days of inactivity
- Settings page SMS Utilities section for manual trigger

## Compliance

- **Google Play**: Finance category. Permission declaration: "This app reads bank transaction SMS to automatically track income/expenses. It does not send, write, or intercept SMS." SMS backup is the utility claim.
- **F-Droid**: Declare `READ_SMS` in metadata. All dependencies open source. Metadata file created at build time.

## User Flow

### First Time (No backup exists)
1. User opens app → authenticates → Dashboard loads
2. MD3 Card appears at top of Dashboard: "SMS Backup Recommended" with "Backup Now" and "Later" buttons
3. User can dismiss → reminder returns in 2 days
4. OR user goes to Settings → SMS Utilities → "Backup & Parse SMS"

### Backup Triggered (Dashboard card, auto-open modal, or Settings)
1. Modal bottom sheet appears: "Backing up your SMS..." with progress indicator
2. Shows live count: "Processing SMS... 340/1,200"
3. On complete → modal transitions to result: "Backup Complete! 1,200 SMS backed up. 45 bank transactions imported."
4. User taps "Done" → modal closes

### Subsequent Opens
- If last backup < 2 days → no reminder, clean Dashboard
- If last backup ≥ 2 days → auto-open backup bottom sheet on app open: "Your SMS backup is 2 days old. Update now?" with "Backup" and "Skip" buttons
- User can dismiss with "Skip" → no card shown until next session

### Settings → SMS Utilities
- Accordion section (collapsible, collapsed by default)
- Status card: last backup date, total SMS backed up, transactions found
- "Backup & Parse SMS" primary button
- "View Backup Files" text button (opens file manager at backup folder)

## Architecture

### New: `SmsBackupParser` — `domain/service/SmsBackupParser.kt`
- `@Singleton`, Hilt-injected
- Dependencies: `Context`, `SmsParser`, `SmsRuleRepository`, `TransactionRepository`, `CategorizationEngine`
- Reads ALL SMS from `ContentResolver.query(Telephony.Sms.CONTENT_URI)` — columns: ADDRESS, BODY, DATE, TYPE
- No 500-message limit — reads entire SMS history
- Writes XML to `context.getExternalFilesDir("sms_backups")/sms_backup_<timestamp>.xml`
- XML format: Android SMS Backup & Restore compatible:
  ```xml
  <smses count="1200">
    <sms protocol="0" address="VM-HDFCBK" date="1685000000000" type="1" body="Your a/c..."/>
    ...
  </smses>
  ```
- For each SMS: matches against active `SmsRuleEntity` (bidirectional contains, same as BroadcastReceiver)
- If match → calls `SmsParser.parse(body, sender, smsDate)` → builds `TransactionEntity` with actual SMS date
- Runs `categorizationEngine.categorize()` after insertion
- Reports progress via callback: `(current: Int, total: Int) -> Unit`
- Returns `BackupResult`

### New: `BackupResult` data class
```kotlin
data class BackupResult(
    val totalSms: Int,
    val bankTransactions: Int,
    val filePath: String,
    val timestamp: Long
)
```

### Updated: `SmsParser` — `domain/service/SmsParser.kt`
Add optional `smsDate` parameter:
```kotlin
fun parse(smsBody: String, senderId: String, smsDate: Long = System.currentTimeMillis()): ParsedTransaction?
```
Use `smsDate` instead of `System.currentTimeMillis()` for the returned `ParsedTransaction.date`.

### Updated: `SettingsViewModel` — `presentation/viewmodel/SettingsViewModel.kt`
Add to `SettingsState`:
- `backupInProgress: Boolean = false`
- `backupProgress: Pair<Int, Int>? = null` — (current, total)
- `lastBackupResult: BackupResult? = null`

New functions:
- `backupAndParseSms()` — launches coroutine, calls `SmsBackupParser.backupAndParse()` on `Dispatchers.IO`, reports progress
- `loadLastBackupInfo()` — reads SharedPreferences for last backup metadata
- `clearBackupProgress()` — resets progress state

SharedPreferences keys: `last_backup_timestamp`, `last_backup_sms_count`, `last_backup_txn_count`, `last_backup_file_path`

### Updated: `DashboardViewModel` (or new `BackupReminderChecker`)
- `showBackupCard: Boolean` — true if no backup exists OR backup > 2 days old
- Check on `init` and `onResume`
- Reads SharedPreferences `last_backup_timestamp`

### Updated: `SmsBroadcastReceiver` — keep but simplified
- Remove `SMS_DELIVER_ACTION` (requires default SMS status)
- Keep `SMS_RECEIVED_ACTION` only — works for real-time if user voluntarily sets app as default
- No changes to parsing logic

## UI Components (Material 3)

### Dashboard Reminder Card
- MD3 `Card` with `CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.08f))`
- Icon: Cloud backup icon from extended icons
- Title: "SMS Backup Recommended" (first time) or "SMS Backup is X days old"
- Subtitle: "Back up SMS to auto-track bank transactions" or "Last backup: May 21 — 1,200 SMS, 45 transactions"
- Buttons: "Backup Now" (`Button`) + "Later" (`TextButton`)
- `AnimatedVisibility` with expand/shrink
- Positioned below TopAppBar, above balance card

### Backup Progress Bottom Sheet
- MD3 `ModalBottomSheet`
- State 1 — Progress: circular progress + "Backing up SMS... 340/1,200" + "Please don't close the app"
- State 2 — Result: checkmark icon + "Backup Complete!" + stats + "Done" button
- Auto-transitions from progress to result
- Called from both Dashboard and Settings

### Settings → SMS Utilities Accordion
- New accordion below SMS Sender Rules
- Header: "SMS Utilities" + subtitle "Backup & manage your SMS"
- Expanded content:
  - Status card (last backup date, SMS count, transactions, file path)
  - "Backup & Parse SMS" primary button
  - "View Backup Files" text button
- Same AnimatedVisibility + chevron pattern as existing accordions

### Settings → Instructions Accordion Update
- Remove "Not set as Default SMS App" warning card
- Remove "Open Default Apps Settings" button
- Remove RoleManager/smsRoleLauncher code
- Update instructions text to describe backup approach

## Manifest Changes (AndroidManifest.xml)

### Remove
- `SENDTO`, `SEND` intent filters from MainActivity
- `CONFIGURE_SMS_DEFAULT_APP` intent filter
- `MmsBroadcastReceiver` declaration
- `RespondViaMessageService` declaration
- `SEND_SMS`, `WRITE_SMS`, `RECEIVE_MMS`, `SEND_RESPOND_VIA_MESSAGE` permissions
- `<queries>` block for MMS

### Keep
- `READ_SMS` — core permission, defensible as financial SMS reader
- `RECEIVE_SMS` — for real-time if user sets as default
- `SmsBroadcastReceiver` with `SMS_RECEIVED` only

### Add
- `FOREGROUND_SERVICE` permission if needed for backup progress

## String Resources
Add to `strings.xml`:
- `sms_utilities_title` — "SMS Utilities"
- `sms_utilities_desc` — "Backup & manage your SMS"
- `sms_backup_parse` — "Backup & Parse SMS"
- `sms_backup_parse_desc` — "Back up all SMS to XML and import bank transactions"
- `sms_backup_progress` — "Backing up SMS..."
- `sms_backup_processing` — "Processing SMS... %1$d/%2$d"
- `sms_backup_please_wait` — "Please don't close the app"
- `sms_backup_complete` — "Backup Complete!"
- `sms_backup_result` — "%1$d SMS backed up. %2$d bank transactions imported."
- `sms_backup_saved` — "Backup saved to %s"
- `sms_backup_reminder_title` — "SMS Backup Recommended"
- `sms_backup_reminder_outdated` — "SMS Backup is %d days old"
- `sms_backup_reminder_desc` — "Back up SMS to auto-track bank transactions"
- `sms_backup_update_prompt` — "Your SMS backup is %d days old. Update now?"
- `sms_backup_last` — "Last backup: %1$s — %2$d SMS, %3$d transactions"
- `btn_backup_now` — "Backup Now"
- `btn_later` — "Later"
- `btn_view_backups` — "View Backup Files"

## Files to Modify/Create/Delete

### Create
- `app/src/main/java/com/deepmoneytracker/domain/service/SmsBackupParser.kt`
- `app/src/main/java/com/deepmoneytracker/data/local/entity/BackupResult.kt` (or inline in SmsBackupParser)

### Modify
- `app/src/main/java/com/deepmoneytracker/domain/service/SmsParser.kt` — add `smsDate` param
- `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt` — backup state/functions
- `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/DashboardViewModel.kt` — backup reminder check
- `app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt` — SMS Utilities accordion, remove default SMS warning
- `app/src/main/java/com/deepmoneytracker/presentation/screens/DashboardScreen.kt` — reminder card, backup bottom sheet
- `app/src/main/java/com/deepmoneytracker/presentation/MainActivity.kt` — remove default SMS prompts
- `app/src/main/AndroidManifest.xml` — cleanup permissions/components
- `app/src/main/res/values/strings.xml` — new string resources

### Delete
- `app/src/main/java/com/deepmoneytracker/data/receiver/RespondViaMessageService.kt`
- `app/src/main/java/com/deepmoneytracker/data/receiver/MmsBroadcastReceiver.kt`

## Verification
1. `./gradlew assembleDebug` — build succeeds
2. Install on real device (Realme Android 11, Motorola Android 15, OPPO Pad Android 13)
3. First launch → Dashboard shows "SMS Backup Recommended" card
4. Tap "Backup Now" → progress bottom sheet → result with SMS count + transactions
5. Verify XML file created in `Android/data/com.deepmoneytracker/files/sms_backups/`
6. Verify bank transactions appear in Transactions tab with correct dates
7. Close and reopen app → no reminder (backup < 2 days old)
8. Settings → SMS Utilities → shows last backup info, "Backup & Parse" works
9. Verify duplicate detection (run backup again → no duplicate transactions)
10. Verify XML contains ALL SMS (not just bank SMS)
11. Verify app no longer shows "Set as Default SMS App" prompts
