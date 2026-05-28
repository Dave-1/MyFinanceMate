# Reminders Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign RemindersScreen and AddReminderScreen to use themeColors, add header action icons, rich cards with type/amount, and styled empty state.

**Architecture:** Follow DashboardScreen's color/typography patterns. Use `themeColors` for all colors, `MaterialTheme.typography` for typography.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `RemindersScreen.kt` | Reminders list + cards | Modify — restyle everything |
| `AddReminderScreen.kt` | Add reminder form | Modify — restyle TopAppBar, cards, button |

---

### Task 1: Restyle RemindersScreen TopAppBar and Scaffold

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt`

- [ ] **Step 1: Add imports**

Add these imports at the top:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepmoneytracker.presentation.theme.LocalThemeColors
```

- [ ] **Step 2: Add themeColors variable**

Add after `val reminders by viewModel.reminders.collectAsStateWithLifecycle()`:

```kotlin
val themeColors = LocalThemeColors.current
```

- [ ] **Step 3: Replace TopAppBar**

Replace the `topBar` block (lines 50-57):

```kotlin
// OLD:
topBar = {
    TopAppBar(
        title = { Text(stringResource(AppStrings.reminders_title), fontWeight = FontWeight.Bold) },
        navigationIcon = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        }
    )
}

// NEW:
topBar = {
    TopAppBar(
        title = {
            Column {
                Text(
                    stringResource(AppStrings.reminders_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                Text(
                    "${reminders.size} reminder${if (reminders.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onBackground.copy(alpha = 0.6f)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* navigate to notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = themeColors.onBackground)
            }
            IconButton(onClick = { /* navigate to reports */ }) {
                Icon(Icons.Default.BarChart, contentDescription = "Reports", tint = themeColors.onBackground)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColors.background,
            titleContentColor = themeColors.onBackground
        )
    )
}
```

- [ ] **Step 4: Add Scaffold containerColor**

Replace `Scaffold(` (line 49):

```kotlin
// OLD:
Scaffold(

// NEW:
Scaffold(
    containerColor = themeColors.background,
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt
git commit -m "feat: restyle RemindersScreen TopAppBar with themeColors and action icons"
```

---

### Task 2: Restyle ReminderCard

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt`

- [ ] **Step 1: Replace ReminderCard composable**

Replace the entire `ReminderCard` function (lines 103-165):

```kotlin
@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Type badge + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (reminder.type == com.deepmoneytracker.data.local.entity.ReminderType.INCOME)
                        themeColors.incomeColor.copy(alpha = 0.15f)
                    else
                        themeColors.expenseColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (reminder.type == com.deepmoneytracker.data.local.entity.ReminderType.INCOME) "Income" else "Expense",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminder.type == com.deepmoneytracker.data.local.entity.ReminderType.INCOME)
                            themeColors.incomeColor
                        else
                            themeColors.expenseColor
                    )
                }

                // Amount
                if (reminder.amount != null) {
                    Text(
                        text = "₹${"%,.2f".format(reminder.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.type == com.deepmoneytracker.data.local.entity.ReminderType.INCOME)
                            themeColors.incomeColor
                        else
                            themeColors.expenseColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = themeColors.onSurface
            )

            // Description
            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Recurrence + Date + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getRecurrenceLabel(reminder.recurrence),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.primary
                    )
                    Text(
                        text = "Next: ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(reminder.nextTriggerTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface.copy(alpha = 0.5f)
                    )
                }

                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete this reminder?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = themeColors.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

- [ ] **Step 2: Add missing imports**

Add these imports:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.deepmoneytracker.data.local.entity.ReminderType
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt
git commit -m "feat: restyle ReminderCard with type badge, amount, and themed colors"
```

---

### Task 3: Restyle empty state

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt`

- [ ] **Step 1: Replace empty state**

Replace the empty state block (lines 64-82):

```kotlin
// OLD:
if (reminders.isEmpty()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No reminders yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap + to add a reminder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// NEW:
if (reminders.isEmpty()) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = themeColors.background)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = themeColors.onBackground.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No reminders yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = themeColors.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    "Tap + to set up payment reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add import for size**

```kotlin
import androidx.compose.foundation.layout.size
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt
git commit -m "feat: restyle RemindersScreen empty state to match Dashboard"
```

---

### Task 4: Restyle AddReminderScreen

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/AddReminderScreen.kt`

- [ ] **Step 1: Add imports**

Add:

```kotlin
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBarDefaults
import com.deepmoneytracker.presentation.theme.LocalThemeColors
```

- [ ] **Step 2: Add themeColors**

Add after `var triggerTime by remember { mutableStateOf(defaultTime) }`:

```kotlin
val themeColors = LocalThemeColors.current
```

- [ ] **Step 3: Replace TopAppBar**

Replace lines 74-83:

```kotlin
// OLD:
topBar = {
    TopAppBar(
        title = { Text(stringResource(AppStrings.add_reminder_title), fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(AppStrings.label_back))
            }
        }
    )
}

// NEW:
topBar = {
    TopAppBar(
        title = {
            Text(
                stringResource(AppStrings.add_reminder_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = themeColors.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(AppStrings.label_back), tint = themeColors.onBackground)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColors.background,
            titleContentColor = themeColors.onBackground
        )
    )
}
```

- [ ] **Step 4: Add Scaffold containerColor**

Replace `Scaffold(` (line 73):

```kotlin
Scaffold(
    containerColor = themeColors.background,
```

- [ ] **Step 5: Replace Card colors**

Replace all three `Card` blocks that use `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))`:

```kotlin
// OLD:
colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
)

// NEW:
colors = CardDefaults.cardColors(
    containerColor = themeColors.cardBackground
)
```

- [ ] **Step 6: Replace section title colors**

Replace all `color = MaterialTheme.colorScheme.primary` in section titles:

```kotlin
// OLD:
color = MaterialTheme.colorScheme.primary

// NEW:
color = themeColors.primary
```

- [ ] **Step 7: Replace Button colors**

Replace the `Button` block (lines 192-210):

```kotlin
// OLD:
Button(
    onClick = { ... },
    modifier = Modifier.fillMaxWidth().height(56.dp),
    enabled = title.isNotBlank()
)

// NEW:
Button(
    onClick = { ... },
    modifier = Modifier.fillMaxWidth().height(56.dp),
    enabled = title.isNotBlank(),
    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
)
```

- [ ] **Step 8: Add ButtonDefaults import**

```kotlin
import androidx.compose.material3.ButtonDefaults
```

- [ ] **Step 9: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/AddReminderScreen.kt
git commit -m "feat: restyle AddReminderScreen with themeColors and AutoMirrored back icon"
```

---

### Task 5: Build and verify

- [ ] **Step 1: Build debug APK**

Run: `./gradlew assembleDebug`

- [ ] **Step 2: Install on device**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual verification**

1. Reminders page uses themeColors throughout
2. Header shows notification + graph icons with subtitle
3. Empty state matches Dashboard style (OutlinedCard with faded text)
4. Cards show type badge, amount, recurrence, date
5. Long-press on card shows delete dialog
6. AddReminder uses themeColors, AutoMirrored back icon, themed button
