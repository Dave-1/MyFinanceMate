# Plan: Update RemindersScreen to use CommonTopAppBar

## Goal
Standardize the UI across all screens by replacing the inline TopAppBar in RemindersScreen.kt with the CommonTopAppBar component.

## Changes Required

### 1. Modify RemindersScreen.kt
**File:** `app/src/main/java/com/deepmoneytracker/presentation/screens/RemindersScreen.kt`

**Replace the `topBar` block (lines 70-99) with:**
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

**Add these imports:**
```kotlin
import com.deepmoneytracker.presentation.components.CommonTopAppBar
import com.deepmoneytracker.presentation.components.TopAppBarAction
```

**Remove unused imports:**
- `import androidx.compose.material3.TopAppBar`
- `import androidx.compose.material3.TopAppBarDefaults`
- `import androidx.compose.ui.text.font.FontWeight` (if only used in TopAppBar)

### 2. Verify Changes
Run `./gradlew assembleDebug` to ensure the app compiles correctly.

### 3. Commit Changes
Commit with message: "refactor: use CommonTopAppBar in RemindersScreen"

## Verification
1. Run `./gradlew assembleDebug` - should succeed
2. Check that the RemindersScreen displays correctly with the new CommonTopAppBar
3. Ensure the subtitle shows the correct reminder count (singular/plural)
4. Verify the action icons (notifications and reports) still work

## Notes
- This is part of a larger effort to standardize UI across all screens
- CommonTopAppBar is already used in DashboardScreen and TransactionsScreen
- The component provides consistent styling and behavior across all screens