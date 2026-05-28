# Common Header & Footer — Design Spec

## Problem

Every screen has its own TopAppBar implementation with slightly different styling, colors, and navigation patterns. The bottom NavigationBar is inline in AppNavigation.kt. This leads to inconsistency (e.g., ReportsScreen uses MaterialTheme.colorScheme instead of themeColors) and code duplication.

## Requirements

1. **Single flexible header** — `CommonTopAppBar` with title, subtitle, back arrow, action icons
2. **Reusable bottom bar** — `CommonBottomBar` extracted from AppNavigation
3. **Consistent styling** — all screens use themeColors through the common components
4. **DRY** — no inline TopAppBar or NavigationBar code in screens

## Changes

### 1. Create CommonTopAppBar.kt

```kotlin
data class TopAppBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
fun CommonTopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: List<TopAppBarAction> = emptyList()
)
```

### 2. Create CommonBottomBar.kt

Move `BottomNavItem` data class and `bottomNavItems` list from `AppNavigation.kt` into this file.

```kotlin
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
)
```

### 3. Update all screens

Replace inline TopAppBar with `CommonTopAppBar`:

| Screen | Title | Subtitle | Nav Icon | Actions |
|---|---|---|---|---|
| Dashboard | app_name | dashboard_subtitle | None | Notifications, Reports |
| Transactions | transactions_title | None | None | None |
| Reminders | reminders_title | count | None | Notifications, Reports |
| Settings | settings_title | None | None | None |
| Categories | label_categories | None | ArrowBack | None |
| AddTransaction | add_transaction_title | None | ArrowBack | None |
| EditTransaction | edit_transaction_title | None | ArrowBack | None |
| AddReminder | add_reminder_title | None | ArrowBack | None |
| Reports | reports_title | None | ArrowBack | None |
| Notifications | notifications_title | None | ArrowBack | DeleteSweep |

### 4. Update AppNavigation.kt

Replace inline NavigationBar with `CommonBottomBar`.

## Files Created

| File | Purpose |
|---|---|
| `CommonTopAppBar.kt` | Flexible header component |
| `CommonBottomBar.kt` | Bottom navigation bar component |

## Files Modified

| File | Changes |
|---|---|
| `DashboardScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `TransactionsScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `RemindersScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `SettingsScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `CategoriesScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `AddTransactionScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `EditTransactionScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `AddReminderScreen.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `ReportsScreen.kt` | Replace inline TopAppBar with CommonTopAppBar, fix themeColors |
| `NotificationsPage.kt` | Replace inline TopAppBar with CommonTopAppBar |
| `AppNavigation.kt` | Replace inline NavigationBar with CommonBottomBar |

## Verification

1. All screens use CommonTopAppBar with consistent styling
2. Bottom bar uses CommonBottomBar
3. Navigation works correctly (back arrows, action icons)
4. ReportsScreen uses themeColors (fixed as part of migration)
5. No inline TopAppBar or NavigationBar code remains
