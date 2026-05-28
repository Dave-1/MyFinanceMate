# Common Header & Footer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract common TopAppBar and BottomNavigationBar into reusable components, replace inline implementations across all screens.

**Architecture:** Create `CommonTopAppBar` and `CommonBottomBar` composables in `presentation/components/`. Replace inline TopAppBar in 10 screens and inline NavigationBar in AppNavigation.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `CommonTopAppBar.kt` | Flexible header component | Create |
| `CommonBottomBar.kt` | Bottom navigation bar | Create |
| `DashboardScreen.kt` | Dashboard | Modify — use CommonTopAppBar |
| `TransactionsScreen.kt` | Transactions list | Modify — use CommonTopAppBar |
| `RemindersScreen.kt` | Reminders list | Modify — use CommonTopAppBar |
| `SettingsScreen.kt` | Settings | Modify — use CommonTopAppBar |
| `CategoriesScreen.kt` | Categories | Modify — use CommonTopAppBar |
| `AddTransactionScreen.kt` | Add transaction | Modify — use CommonTopAppBar |
| `EditTransactionScreen.kt` | Edit transaction | Modify — use CommonTopAppBar |
| `AddReminderScreen.kt` | Add reminder | Modify — use CommonTopAppBar |
| `ReportsScreen.kt` | Reports | Modify — use CommonTopAppBar, fix themeColors |
| `NotificationsPage.kt` | Notifications | Modify — use CommonTopAppBar |
| `AppNavigation.kt` | Navigation | Modify — use CommonBottomBar |

---

### Task 1: Create CommonTopAppBar

**Files:**
- Create: `app/src/main/java/com/deepmoneytracker/presentation/components/CommonTopAppBar.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.deepmoneytracker.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.deepmoneytracker.presentation.theme.AppStrings
import com.deepmoneytracker.presentation.theme.LocalThemeColors

data class TopAppBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: List<TopAppBarAction> = emptyList()
) {
    val themeColors = LocalThemeColors.current

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        navigationIcon,
                        contentDescription = stringResource(AppStrings.label_back),
                        tint = themeColors.onBackground
                    )
                }
            }
        },
        actions = {
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        action.icon,
                        contentDescription = action.contentDescription,
                        tint = themeColors.onBackground
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColors.background,
            titleContentColor = themeColors.onBackground
        )
    )
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/components/CommonTopAppBar.kt
git commit -m "feat: create CommonTopAppBar reusable component"
```

---

### Task 2: Create CommonBottomBar

**Files:**
- Create: `app/src/main/java/com/deepmoneytracker/presentation/components/CommonBottomBar.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.deepmoneytracker.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.deepmoneytracker.presentation.navigation.Screen
import com.deepmoneytracker.presentation.theme.LocalThemeColors

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Filled.Home, Screen.Dashboard.route),
    BottomNavItem("Transactions", Icons.Outlined.Payments, Icons.Outlined.Payments, Screen.Transactions.route),
    BottomNavItem("Reminders", Icons.Filled.Notifications, Icons.Filled.Notifications, Screen.Reminders.route),
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Filled.Settings, Screen.Settings.route)
)

@Composable
fun CommonBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val themeColors = LocalThemeColors.current

    NavigationBar(
        containerColor = themeColors.surface,
        contentColor = themeColors.onSurface
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        fontSize = 11.sp
                    )
                },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = themeColors.primary,
                    selectedTextColor = themeColors.primary,
                    unselectedIconColor = themeColors.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = themeColors.onSurface.copy(alpha = 0.6f),
                    indicatorColor = themeColors.primary.copy(alpha = 0.15f)
                ),
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/components/CommonBottomBar.kt
git commit -m "feat: create CommonBottomBar reusable component"
```

---

### Task 3: Update AppNavigation to use CommonBottomBar

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt`

- [ ] **Step 1: Remove inline NavigationBar code**

Remove the `bottomNavItems` list (lines 51-56), `BottomNavItem` data class (lines 44-49), and the entire `bottomBar` block (lines 71-123).

- [ ] **Step 2: Replace with CommonBottomBar**

Add inside Scaffold, after the closing `}` of `NavHost`:

```kotlin
bottomBar = {
    if (showBottomBar) {
        CommonBottomBar(
            currentRoute = currentDestination?.route,
            onNavigate = { route ->
                if (navController.currentDestination?.route != route) {
                    if (route == Screen.Dashboard.route) {
                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        )
    }
}
```

- [ ] **Step 3: Update imports**

Remove unused imports, add:
```kotlin
import com.deepmoneytracker.presentation.components.CommonBottomBar
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/navigation/AppNavigation.kt
git commit -m "refactor: use CommonBottomBar in AppNavigation"
```

---

### Task 4: Update DashboardScreen to use CommonTopAppBar

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/DashboardScreen.kt`

- [ ] **Step 1: Replace TopAppBar**

Replace the `topBar` block with:

```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.app_name),
        subtitle = stringResource(AppStrings.dashboard_subtitle),
        actions = listOf(
            TopAppBarAction(Icons.Default.Notifications, stringResource(AppStrings.label_notifications), onNavigateToNotifications),
            TopAppBarAction(Icons.Default.BarChart, stringResource(AppStrings.label_reports), onNavigateToReports)
        )
    )
}
```

- [ ] **Step 2: Update imports**

Add:
```kotlin
import com.deepmoneytracker.presentation.components.CommonTopAppBar
import com.deepmoneytracker.presentation.components.TopAppBarAction
```

Remove unused TopAppBar-related imports.

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/DashboardScreen.kt
git commit -m "refactor: use CommonTopAppBar in DashboardScreen"
```

---

### Task 5: Update TransactionsScreen to use CommonTopAppBar

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt`

- [ ] **Step 1: Replace TopAppBar**

Replace the `topBar` block with:

```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.transactions_title)
    )
}
```

- [ ] **Step 2: Update imports**

Add CommonTopAppBar import, remove unused TopAppBar imports.

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git commit -m "refactor: use CommonTopAppBar in TransactionsScreen"
```

---

### Task 6: Update RemindersScreen to use CommonTopAppBar

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt`

- [ ] **Step 1: Replace TopAppBar**

Replace the `topBar` block with:

```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.reminders_title),
        subtitle = "${reminders.size} ${stringResource(if (reminders.size != 1) AppStrings.reminders_count_plural else AppStrings.reminders_count)}",
        actions = listOf(
            TopAppBarAction(Icons.Default.Notifications, stringResource(AppStrings.label_notifications), onNavigateToNotifications),
            TopAppBarAction(Icons.Default.BarChart, stringResource(AppStrings.label_reports), onNavigateToReports)
        )
    )
}
```

- [ ] **Step 2: Update imports**

Add CommonTopAppBar and TopAppBarAction imports, remove unused TopAppBar imports.

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt
git commit -m "refactor: use CommonTopAppBar in RemindersScreen"
```

---

### Task 7: Update SettingsScreen to use CommonTopAppBar

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt`

- [ ] **Step 1: Replace TopAppBar**

Replace the `topBar` block with:

```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.settings_title)
    )
}
```

- [ ] **Step 2: Update imports**

Add CommonTopAppBar import, remove unused TopAppBar imports.

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/SettingsScreen.kt
git commit -m "refactor: use CommonTopAppBar in SettingsScreen"
```

---

### Task 8: Update sub-pages (Categories, AddTransaction, EditTransaction, AddReminder)

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/CategoriesScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/AddTransactionScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/EditTransactionScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/AddReminderScreen.kt`

- [ ] **Step 1: Update CategoriesScreen**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.label_categories),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack
    )
}
```

Add CommonTopAppBar import, remove unused TopAppBar imports.

- [ ] **Step 2: Update AddTransactionScreen**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.add_transaction_title),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack
    )
}
```

- [ ] **Step 3: Update EditTransactionScreen**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.edit_transaction_title),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack
    )
}
```

- [ ] **Step 4: Update AddReminderScreen**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.add_reminder_title),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack
    )
}
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/CategoriesScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/AddTransactionScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/EditTransactionScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/AddReminderScreen.kt
git commit -m "refactor: use CommonTopAppBar in sub-pages"
```

---

### Task 9: Update ReportsScreen and NotificationsPage

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/ReportsScreen.kt`
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt`

- [ ] **Step 1: Update ReportsScreen**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.reports_title),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack
    )
}
```

Also fix: Add `containerColor = themeColors.background` to Scaffold, replace `MaterialTheme.colorScheme` usages with `themeColors` where applicable.

- [ ] **Step 2: Update NotificationsPage**

Replace TopAppBar with:
```kotlin
topBar = {
    CommonTopAppBar(
        title = stringResource(AppStrings.notifications_title),
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onNavigateBack,
        actions = listOf(
            TopAppBarAction(Icons.Default.DeleteSweep, stringResource(AppStrings.notifications_clear_read)) { /* clear read */ }
        )
    )
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/ReportsScreen.kt
git add app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt
git commit -m "refactor: use CommonTopAppBar in ReportsScreen and NotificationsPage"
```

---

### Task 10: Build and verify

- [ ] **Step 1: Build debug APK**

Run: `./gradlew assembleDebug`

- [ ] **Step 2: Install on device**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual verification**

1. All screens use CommonTopAppBar with consistent styling
2. Bottom bar uses CommonBottomBar
3. Navigation works (back arrows, action icons, bottom nav)
4. No inline TopAppBar or NavigationBar code remains
