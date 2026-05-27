# SMS Utility App Refinement — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 4 bugs and enhance the SMS utility app with backup, parsing, notifications, and UI polish across 4 phases.

**Architecture:** Phased implementation — each phase builds on the previous. Phase 1 fixes existing bugs, Phase 2 adds SMS backup/parsing, Phase 3 overhauls notifications, Phase 4 polishes UI.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Hilt, WorkManager

---

## File Structure

| File | Responsibility |
|------|----------------|
| `AppNavigation.kt` | Bottom nav bar, nav host, tab routing |
| `TransactionsScreen.kt` | Transaction list with filter chips |
| `TransactionViewModel.kt` | Transaction state, filtering, search |
| `NotificationsPage.kt` | Notification list with category chips |
| `NotificationsViewModel.kt` | Notification state, category filtering |
| `PinDialog.kt` | SetPinDialog, VerifyPinDialog composables |
| `LockScreen.kt` | App lock screen with biometric/PIN |
| `SettingsScreen.kt` | Settings with backup, rules, security |
| `SettingsViewModel.kt` | Settings state, backup/restore actions |
| `SmsBackupParser.kt` | SMS backup XML + parse logic |
| `SmsNotificationWorker.kt` | Background worker for notifications |
| `DashboardScreen.kt` | Dashboard with recent transactions |
| `DashboardViewModel.kt` | Dashboard state, backup reminder |

---

## Phase 1: Bug Fixes

### Task 1: Fix Transactions Label Wrapping

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt:88-109`

- [ ] **Step 1: Add maxLines and overflow to NavigationBarItem label**

In `AppNavigation.kt`, find the `NavigationBarItem` inside `bottomNavItems.forEach`. The `label` parameter currently has no text constraints. Add `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, and `softWrap = false`:

```kotlin
NavigationBarItem(
    icon = { /* existing icon code */ },
    label = {
        Text(
            text = item.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            fontSize = 11.sp
        )
    },
    // ... rest unchanged
)
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt
git commit -m "fix: prevent bottom nav label wrapping on small screens"
```

---

### Task 2: Fix SMS Chip Filtering in NotificationsPage

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/NotificationsViewModel.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt`

- [ ] **Step 1: Fix stale data in NotificationsViewModel**

In `NotificationsViewModel.kt`, change `SharingStarted.WhileSubscribed(5000)` to `SharingStarted.Eagerly` to prevent state loss when navigating away and back:

```kotlin
val state: StateFlow<NotificationsState> = combine(
    smsNotificationRepository.getAll(),
    smsNotificationRepository.getUnreadCount(),
    selectedCategory
) { allNotifications, unreadCount, filter ->
    // ... existing combine logic unchanged
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = NotificationsState()
)
```

- [ ] **Step 2: Add filter reset on page load in NotificationsPage**

In `NotificationsPage.kt`, add a `LaunchedEffect` at the top of the composable to reset the filter when the page first loads:

```kotlin
@Composable
fun NotificationsPage(
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.setCategory(null)
    }

    // ... rest unchanged
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/NotificationsViewModel.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt
git commit -m "fix: prevent notification chip filter state loss on navigation"
```

---

### Task 3: Fix Rotation Handling

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt:69`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt` (if has similar state)

- [ ] **Step 1: Replace remember with rememberSaveable for filter state**

In `TransactionsScreen.kt`, change line 69 from:

```kotlin
var selectedFilter by remember { mutableStateOf<String?>(null) }
```

to:

```kotlin
var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }
```

Add import at top of file:

```kotlin
import androidx.compose.runtime.saveable.rememberSaveable
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git commit -m "fix: preserve filter state across rotation using rememberSaveable"
```

---

### Task 4: Fix PIN Modal Auto-Close

**Files:**
- Verify: `app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt:127-136`
- Verify: `app/src/main/java/com/deepmoneytracker/presentation/components/PinDialog.kt`

- [ ] **Step 1: Verify LockScreen handles VerifyPinDialog correctly**

The `LockScreen.kt` already correctly dismisses the dialog:

```kotlin
if (showVerifyPin) {
    VerifyPinDialog(
        onDismiss = { /* Cannot dismiss - must verify */ },
        onVerified = {
            showVerifyPin = false  // <-- already dismisses
            onAuthenticated()
        },
        onVerify = { pin -> pinAuthManager.verifyPin(pin) }
    )
}
```

The `VerifyPinDialog` in `PinDialog.kt` calls `onVerified()` when verification succeeds. This is correct. The PIN modal auto-close is already working in `LockScreen`.

Check if there's another caller (e.g., `SettingsScreen`) that doesn't dismiss properly. If `SettingsScreen` uses `VerifyPinDialog`, ensure its `onVerified` callback sets `showVerifyPin = false`.

- [ ] **Step 2: If SettingsScreen has VerifyPinDialog, fix it**

In `SettingsScreen.kt`, if there's a `VerifyPinDialog` usage, ensure:

```kotlin
if (showVerifyPin) {
    VerifyPinDialog(
        onDismiss = { showVerifyPin = false },
        onVerified = {
            showVerifyPin = false
            // ... other action
        },
        onVerify = { pin -> viewModel.verifyPin(pin) }
    )
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/LockScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt
git commit -m "fix: ensure PIN modal dismisses on correct verification"
```

---

## Phase 2: SMS Backup & Parsing

### Task 5: Add One-Click SMS Backup Button

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: Add SMS backup state to SettingsViewModel**

In `SettingsViewModel.kt`, add state for SMS backup:

```kotlin
data class SettingsState(
    // ... existing fields
    val smsBackupInProgress: Boolean = false,
    val smsBackupResult: BackupResult? = null,
    val smsBackupError: String? = null
)
```

Add backup function:

```kotlin
fun backupSms() {
    viewModelScope.launch {
        _state.update { it.copy(smsBackupInProgress = true, smsBackupError = null) }
        try {
            val result = smsBackupParser.backupAndParse()
            _state.update { it.copy(smsBackupInProgress = false, smsBackupResult = result) }
        } catch (e: Exception) {
            _state.update { it.copy(smsBackupInProgress = false, smsBackupError = e.message) }
        }
    }
}
```

- [ ] **Step 2: Add SMS Backup button in SettingsScreen**

In `SettingsScreen.kt`, add a new "SMS Utilities" section after the "Backup & Restore" section:

```kotlin
// SMS Utilities
item {
    Spacer(modifier = Modifier.height(4.dp))
    Text("SMS Utilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
    Text("Backup and parse SMS messages", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
}

item {
    Button(
        onClick = { viewModel.backupSms() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.smsBackupInProgress
    ) {
        if (state.smsBackupInProgress) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Backup, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Backup & Parse SMS")
        }
    }
}

// Show result if available
state.smsBackupResult?.let { result ->
    item {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backup Complete", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = themeColors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total SMS: ${result.totalSms}", style = MaterialTheme.typography.bodyMedium)
                Text("Bank transactions: ${result.bankTransactions}", style = MaterialTheme.typography.bodyMedium)
                Text("Notifications: ${result.notifications}", style = MaterialTheme.typography.bodyMedium)
                if (result.bankBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bank breakdown:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    result.bankBreakdown.forEach { (bank, count) ->
                        Text("  $bank: $count", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt
git commit -m "feat: add one-click SMS backup button in Settings"
```

---

### Task 6: Add View Backup Files

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: Add backup files list to SettingsViewModel**

```kotlin
// Add to SettingsState
val backupFiles: List<BackupFileInfo> = emptyList()

data class BackupFileInfo(
    val name: String,
    val date: String,
    val size: String,
    val path: String
)

fun loadBackupFiles() {
    viewModelScope.launch {
        val files = smsBackupParser.getBackupFiles()
        _state.update { it.copy(backupFiles = files) }
    }
}
```

- [ ] **Step 2: Add "View Backups" button and file list in SettingsScreen**

```kotlin
item {
    OutlinedButton(
        onClick = { viewModel.loadBackupFiles() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("View Backup Files")
    }
}

if (state.backupFiles.isNotEmpty()) {
    items(state.backupFiles) { file ->
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("${file.date} • ${file.size}", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = { /* share via FileProvider */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/SettingsViewModel.kt
git commit -m "feat: add view backup files list in Settings"
```

---

### Task 7: Add Per-Bank Sub-Tabs to TransactionsScreen

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/viewmodel/TransactionViewModel.kt`

- [ ] **Step 1: Add bank names list to TransactionViewModel**

```kotlin
// Add to TransactionViewModel
val bankNames: StateFlow<List<String>> = smsRuleRepository.getAllActive()
    .map { rules -> rules.map { it.senderName }.filter { it.isNotBlank() }.distinct() }
    .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList())
```

- [ ] **Step 2: Extend filter logic in TransactionsScreen**

Change `selectedFilter` from `String?` to handle bank names:

```kotlin
var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }
val bankNames by viewModel.bankNames.collectAsStateWithLifecycle()

val filteredTransactions = when (selectedFilter) {
    "Income" -> transactions.filter { it.type == TransactionType.INCOME }
    "Expense" -> transactions.filter { it.type == TransactionType.EXPENSE }
    "SMS" -> transactions.filter { it.isFromSms }
    null -> transactions
    else -> {
        // Bank name filter
        val bankName = selectedFilter
        transactions.filter { it.senderInfo?.contains(bankName ?: "", ignoreCase = true) == true }
    }
}
```

- [ ] **Step 3: Add SMS and bank chips to filter row**

```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    item {
        FilterChipCustom(label = "All", selected = selectedFilter == null, onClick = { selectedFilter = null }, themeColors = themeColors)
    }
    item {
        FilterChipCustom(label = "Income", selected = selectedFilter == "Income", onClick = { selectedFilter = if (selectedFilter == "Income") null else "Income" }, themeColors = themeColors)
    }
    item {
        FilterChipCustom(label = "Expense", selected = selectedFilter == "Expense", onClick = { selectedFilter = if (selectedFilter == "Expense") null else "Expense" }, themeColors = themeColors)
    }
    item {
        FilterChipCustom(label = "SMS", selected = selectedFilter == "SMS", onClick = { selectedFilter = if (selectedFilter == "SMS") null else "SMS" }, themeColors = themeColors)
    }
    items(bankNames) { bankName ->
        FilterChipCustom(
            label = bankName,
            selected = selectedFilter == bankName,
            onClick = { selectedFilter = if (selectedFilter == bankName) null else bankName },
            themeColors = themeColors
        )
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/viewmodel/TransactionViewModel.kt
git commit -m "feat: add SMS and per-bank filter chips to Transactions"
```

---

## Phase 3: Notifications Overhaul

### Task 8: Fix Notification Sorting

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt`

- [ ] **Step 1: Update sorting logic**

In `NotificationsPage.kt`, change the sorting from simple date descending to priority-based:

```kotlin
// Current (line ~168):
// (state.expiredNotifications + state.notifications).sortedByDescending { it.smsDate }

// New:
val sortedNotifications = buildList {
    // Priority 1: Unread + expired (pinned to top)
    addAll(state.expiredNotifications.filter { !it.isRead }.sortedByDescending { it.smsDate })
    // Priority 2: Unread + regular
    addAll(state.notifications.filter { !it.isRead }.sortedByDescending { it.smsDate })
    // Priority 3: Read items
    addAll(state.expiredNotifications.filter { it.isRead }.sortedByDescending { it.smsDate })
    addAll(state.notifications.filter { it.isRead }.sortedByDescending { it.smsDate })
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt
git commit -m "fix: sort notifications by priority (unread+expired first) then date"
```

---

### Task 9: Add Empty State for Filtered Notifications

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt`

- [ ] **Step 1: Add empty state when filter returns no results**

After the `sortedNotifications` calculation, add:

```kotlin
if (sortedNotifications.isEmpty() && state.selectedCategory != null) {
    // Filtered empty state
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = themeColors.primary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No ${getCategoryLabel(state.selectedCategory!!)} notifications",
                style = MaterialTheme.typography.titleMedium,
                color = themeColors.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Try selecting a different category",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt
git commit -m "feat: add empty state for filtered notification category"
```

---

## Phase 4: UI Polish

### Task 10: Add Swipe Actions to TransactionsScreen

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt`

- [ ] **Step 1: Wrap TransactionCard with SwipeToDismissBox**

In the `DateAccordionList` item rendering, wrap each `TransactionCard` with swipe gestures:

```kotlin
// Inside DateAccordionList's itemContent lambda:
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.StartToEnd -> {
                // Right swipe: no action (or mark as read if applicable)
                false
            }
            SwipeToDismissBoxValue.EndToStart -> {
                // Left swipe: delete
                viewModel.deleteTransaction(transaction)
                true
            }
            SwipeToDismissBoxValue.Settled -> false
        }
    }
)

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
        val color = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.EndToStart -> themeColors.error
            else -> Color.Transparent
        }
        Box(
            modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
        }
    },
    enableDismissFromStartToEnd = false
) {
    TransactionCard(transaction = transaction, ...)
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git commit -m "feat: add swipe-to-delete on transaction cards"
```

---

### Task 11: Material 3 Compliance Audit

**Files:**
- All screen files

- [ ] **Step 1: Verify consistent color usage**

Check all screens use `themeColors.*` instead of `MaterialTheme.colorScheme.*`. The app uses custom `ThemeColors` via `LocalThemeColors.current`. Ensure no screen uses the raw Material theme directly.

- [ ] **Step 2: Verify card corner radius**

All cards should use `RoundedCornerShape(14.dp)`. Check `CardDefaults.cardColors(containerColor = themeColors.cardBackground)` is used consistently.

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit if changes needed**

```bash
git add -A
git commit -m "fix: ensure consistent Material 3 compliance across screens"
```

---

## Summary

| Task | Phase | Description |
|------|-------|-------------|
| 1 | Bug Fix | Transactions label wrapping |
| 2 | Bug Fix | SMS chip filtering |
| 3 | Bug Fix | Rotation handling |
| 4 | Bug Fix | PIN modal auto-close |
| 5 | Backup | One-click SMS backup |
| 6 | Backup | View backup files |
| 7 | Parsing | Per-bank sub-tabs |
| 8 | Notifications | Sorting fix |
| 9 | Notifications | Empty state for filters |
| 10 | UI Polish | Swipe actions |
| 11 | UI Polish | M3 compliance audit |
