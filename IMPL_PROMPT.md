# Implementation Prompt: Accounts, Salary Detection, and Fixed-Expense Detection

## Context

You are working on **MyFinanceMate**, a privacy-first Android expense tracking
app at `https://github.com/Dave-1/MyFinanceMate`. The app parses bank SMS
alerts, stores data locally with Room, and visualizes spending via charts.
Stack: Kotlin + Jetpack Compose, AGP 8.7.3, Kotlin 2.1.0, Room, Hilt, KSP.

The source code is at:
```
C:\Users\Navnath\DailyWork\projects\projects-using-opencode\MyFinanceMate
```

The companion F-Droid metadata fork is at:
```
C:\Users\Navnath\DailyWork\projects\projects-using-opencode\fdroiddata
```

A mapping document explaining how both repos relate lives at:
```
MyFinanceMate/F-DROID-MAPPING.md
```

## What to implement

### Feature 1: Multi-Account Support

**Goal:** Users with multiple bank accounts should have each account tracked
separately, with one account designated "primary."

**How primary is determined:** The account where salary credit SMS arrives
becomes primary automatically. The user can also override this manually.

**Data model:**
- New `AccountEntity` table: `id, name, bankName, senderId, accountSuffix (last4
  digits, optional), isPrimary, createdAt`
- New `accountId: Long?` column on `TransactionEntity` (FK → accounts.id,
  SET_NULL on delete)
- New `isSalary: Boolean = false` column on `TransactionEntity`
- New `FixedExpenseConfig` table (singleton): `id (=1), minOccurrences (=3),
  variancePercent (=10.0)` — user-configurable thresholds
- Add `sourceAccountId: Long?` to `ReminderEntity` (not a FK)

**Services:**
- `AccountResolutionService` — resolves sender ID to account, auto-creates
  accounts on first SMS
- `SalaryDetector` — checks if parsed INCOME matches a salary rule (reuses
  existing `SmsRuleEntity` with new `ruleType` enum field)

**UI:**
- New `AccountsScreen` (accessible from Settings) — list accounts, set primary,
  add/rename/delete
- Add account filter chip to `TransactionsScreen`
- Add primary account indicator to `DashboardScreen`
- Add account dropdown to `AddTransactionScreen` and `EditTransactionScreen`
- Add "Accounts" section + "Fixed Expense Detection" section to `SettingsScreen`

### Feature 2: Fixed-Expense Auto-Detection

**Goal:** When an expense recurs monthly with consistent amounts, auto-detect
it and create a recurring `ReminderEntity` (MONTHLY).

**Detection algorithm (in `FixedExpenseDetector`):**
1. After a new EXPENSE transaction is inserted, group all expenses by
   `accountId + merchant + categoryId`
2. Bucket by calendar month
3. If distinct months >= `minOccurrences` (default 3):
   - Check amount variance <= `variancePercent` (default 10%)
   - Compute average amount and average day-of-month
   - Create/update a `ReminderEntity` with `Recurrence.MONTHLY` and
     `sourceAccountId` set
4. Idempotent: checks for duplicate reminders before creating

**UI:**
- Visual "AUTO" chip on auto-detected reminders in `RemindersScreen`
- Configurable thresholds (min months, max variance %) in `SettingsScreen`

### Feature 3: SMS Rules Extended

**Goal:** Extend `SmsRuleEntity` to support salary detection via existing UI.

**Change:** Add `ruleType` enum field:
```kotlin
enum class SmsRuleType { TRANSACTION, SALARY, FIXED_EXPENSE }
```
Default is `TRANSACTION` so all existing rules keep working. The SettingsScreen
SMS rules UI gets a small "Type" dropdown when creating a new rule.

## SMS sample for testing

A real SMS backup XML with 5702 messages lives at:
```
MyFinanceMate/sms_backup_20260717_111345.xml
```
Use this to verify the detectors work against real Indian bank SMS formats.
Key sender prefixes in the data include: `AD-TVSMTR`, `VK-MSEDCL`, `VM-MSEDCL`,
`VK-FLPKRT`, `VM-NSESMS`, `VK-CDSLEV`, `BP-FLXLON`, `AD-TRAIND`, and many
phone-number senders. Bank transaction senders will be identifiable by SMS body
containing debit/credit keywords and amounts in ₹.

## Phase order (implement in this sequence)

### Phase 1: Data Layer

1. `data/local/entity/AccountEntity.kt` — NEW entity
2. `data/local/entity/FixedExpenseConfig.kt` — NEW entity
3. `data/local/entity/SmsRuleEntity.kt` — EDIT: add `ruleType` field + enum
4. `data/local/entity/TransactionEntity.kt` — EDIT: add `accountId`, `isSalary`
5. `data/local/entity/ReminderEntity.kt` — EDIT: add `sourceAccountId`
6. `data/local/dao/AccountDao.kt` — NEW DAO
7. `data/local/dao/FixedExpenseConfigDao.kt` — NEW DAO
8. `data/local/dao/TransactionDao.kt` — EDIT: add queries for account grouping
9. `data/local/dao/SmsRuleDao.kt` — EDIT: add `getSalaryRules()`
10. `data/local/dao/ReminderDao.kt` — EDIT: add account filtering + dedup query
11. `data/local/converter/Converters.kt` — EDIT: add SmsRuleType converter
12. `data/local/AppDatabase.kt` — EDIT: add entities, DAOs, bump version to 3,
    add `Migration(2, 3)` with the SQL above
13. `domain/repository/AccountRepository.kt` — NEW interface
14. `domain/repository/FixedExpenseConfigRepository.kt` — NEW interface
15. `domain/repository/TransactionRepository.kt` — EDIT: add new query methods
16. `domain/repository/SmsRuleRepository.kt` — EDIT: add `getSalaryRules()`
17. `data/repository/AccountRepositoryImpl.kt` — NEW
18. `data/repository/FixedExpenseConfigRepositoryImpl.kt` — NEW
19. `data/repository/TransactionRepositoryImpl.kt` — EDIT: implement new methods
20. `data/repository/SmsRuleRepositoryImpl.kt` — EDIT: implement new method

### Phase 2: Domain Services

21. `domain/service/AccountResolutionService.kt` — NEW singleton
22. `domain/service/SalaryDetector.kt` — NEW singleton
23. `domain/service/FixedExpenseDetector.kt` — NEW singleton (most complex)

### Phase 3: Ingestion Path Changes

24. `data/receiver/SmsBroadcastReceiver.kt` — EDIT: inject new services,
    attach accountId + isSalary, run fixed-expense detection
25. `domain/service/SmsBackupParser.kt` — EDIT: same as receiver, plus
    post-processing pass over all transaction groups

### Phase 4: DI Wiring

26. `di/AppModule.kt` — EDIT: add AccountDao, FixedExpenseConfigDao providers,
    AccountRepository, FixedExpenseConfigRepository bindings
27. `di/DatabaseInitializer.kt` — EDIT: seed default FixedExpenseConfig row

### Phase 5: Presentation Layer

28. `presentation/navigation/Screen.kt` — EDIT: add `Accounts` route
29. `presentation/navigation/AppNavigation.kt` — EDIT: add Accounts composable
30. `presentation/viewmodel/AccountViewModel.kt` — NEW HiltViewModel
31. `presentation/screens/AccountsScreen.kt` — NEW Compose screen
32. `presentation/viewmodel/SettingsViewModel.kt` — EDIT: add accounts state,
    fixed-expense config, account navigation
33. `presentation/screens/SettingsScreen.kt` — EDIT: add Accounts section +
    Fixed Expense Detection section with sliders
34. `presentation/viewmodel/DashboardViewModel.kt` — EDIT: add primary account
35. `presentation/screens/DashboardScreen.kt` — EDIT: show primary account card
36. `presentation/screens/AddTransactionScreen.kt` — EDIT: account dropdown
37. `presentation/screens/EditTransactionScreen.kt` — EDIT: account dropdown
38. `presentation/screens/TransactionsScreen.kt` — EDIT: account filter chip
39. `presentation/screens/RemindersScreen.kt` — EDIT: "AUTO" chip for
    auto-detected reminders
40. `presentation/theme/AppStrings.kt` — EDIT: new string resources

### Phase 6: Build + Metadata

41. `app/build.gradle.kts` — EDIT: bump versionCode to 3, versionName to 1.2.0
42. `.github/workflows/build-release.yml` — EDIT: fix hardcoded version to use
    `${{ github.ref_name }}` dynamically
43. `metadata/com.myfinancemate.yml` — EDIT: add 1.2.0 build entry
44. `f-droid/config.yml` — EDIT: update version + changelog
45. `fdroiddata/metadata/com.myfinancemate.yml` — EDIT: mirror the above

### Phase 7: Tests

46. `app/src/test/.../SalaryDetectorTest.kt` — NEW JVM unit test
47. `app/src/test/.../AccountResolutionServiceTest.kt` — NEW JVM unit test
48. `app/src/test/.../FixedExpenseDetectorTest.kt` — NEW JVM unit test

## Room Migration SQL (version 2 → 3)

```sql
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    bankName TEXT NOT NULL DEFAULT '',
    senderId TEXT NOT NULL,
    accountSuffix TEXT NOT NULL DEFAULT '',
    isPrimary INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL
);
CREATE INDEX idx_accounts_senderId ON accounts(senderId);
CREATE INDEX idx_accounts_isPrimary ON accounts(isPrimary);

ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL;
ALTER TABLE transactions ADD COLUMN isSalary INTEGER NOT NULL DEFAULT 0;
CREATE INDEX idx_transactions_accountId ON transactions(accountId);

CREATE TABLE fixed_expense_config (
    id INTEGER PRIMARY KEY,
    minOccurrences INTEGER NOT NULL DEFAULT 3,
    variancePercent REAL NOT NULL DEFAULT 10.0
);

ALTER TABLE reminders ADD COLUMN sourceAccountId INTEGER DEFAULT NULL;
```

## Design decisions already made

1. **Account model:** Full `AccountEntity` with manual override (not
   sender-based, not automatic-only)
2. **Salary detection:** Reuses existing `SmsRuleEntity` with new `ruleType`
   field — user creates "SALARY" type rules in the existing SMS Rules UI
3. **Fixed-expense thresholds:** Default 3 months / 10% variance, but
   user-configurable via Settings sliders (min 2–6 months, 5–25% variance)
4. **Fixed-expense detector:** Groups by `accountId + merchant + categoryId`,
   checks consecutive months, idempotent (no duplicate reminders)
5. **Auto-account creation:** Accounts are created automatically on first SMS
   from a new sender; first-ever account auto-becomes primary
6. **No new bottom nav tab:** Accounts management lives inside Settings

## Patterns to follow

- Compose screens: `hiltViewModel()`, `collectAsStateWithLifecycle()`,
  `LocalThemeColors.current`, `CommonTopAppBar` + `CommonBottomBar`
- ViewModels: `@HiltViewModel`, `StateFlow` with
  `SharingStarted.WhileSubscribed(5000)`
- Room entities: `@Entity`, `@PrimaryKey(autoGenerate = true)`, foreign keys
  with `onDelete = ForeignKey.SET_NULL`
- DI: `@Singleton` services with `@Inject constructor` are auto-discovered by
  Hilt; DAOs need explicit `@Provides` in `AppModule`

## Important files to read first

Before writing any code, read these to understand the existing patterns:
- `app/src/main/java/com/myfinancemate/data/local/AppDatabase.kt`
- `app/src/main/java/com/myfinancemate/data/local/entity/TransactionEntity.kt`
- `app/src/main/java/com/myfinancemate/data/local/entity/SmsRuleEntity.kt`
- `app/src/main/java/com/myfinancemate/data/local/entity/ReminderEntity.kt`
- `app/src/main/java/com/myfinancemate/data/local/dao/TransactionDao.kt`
- `app/src/main/java/com/myfinancemate/domain/service/SmsParser.kt`
- `app/src/main/java/com/myfinancemate/domain/service/SmsBackupParser.kt`
- `app/src/main/java/com/myfinancemate/data/receiver/SmsBroadcastReceiver.kt`
- `app/src/main/java/com/myfinancemate/di/AppModule.kt`
- `app/src/main/java/com/myfinancemate/presentation/navigation/Screen.kt`
- `app/src/main/java/com/myfinancemate/presentation/screens/SettingsScreen.kt`
- `app/src/main/java/com/myfinancemate/presentation/viewmodel/SettingsViewModel.kt`
- `app/src/main/java/com/myfinancemate/presentation/screens/DashboardScreen.kt`
- `app/src/main/java/com/myfinancemate/presentation/viewmodel/DashboardViewModel.kt`
- `app/src/main/java/com/myfinancemate/F-DROID-MAPPING.md`

## Verification

After implementation:
1. Build compiles: `./gradlew assembleDebug`
2. Unit tests pass: `./gradlew test`
3. Test against the SMS sample by loading `sms_backup_20260717_111345.xml` via
   the Settings > "Parse SMS Backup" button in the app
4. Verify accounts are auto-created from sender IDs
5. Verify salary SMS marks its account as primary
6. Verify recurring expenses produce monthly reminders
7. Run the existing proguard/lint checks: `./gradlew lint`
