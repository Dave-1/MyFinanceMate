# Reminders Page Redesign — Design Spec

## Problem

The Reminders page uses Material 3 defaults instead of the app's custom `themeColors`, has no action icons in the header, plain empty state, and basic card layout without type/amount indicators.

## Requirements

1. **TopAppBar:** Use `themeColors`, add notification + graph action icons, subtitle with reminder count
2. **ReminderCard:** Rich layout with type badge (income/expense), amount, recurrence chip, date, toggle
3. **Empty state:** Styled `OutlinedCard` with icon and text (matches Dashboard pattern)
4. **AddReminderScreen:** Use `themeColors`, AutoMirrored back icon, themed cards and button
5. **Consistent styling:** Follow DashboardScreen's color/typography patterns

## Changes

### 1. RemindersScreen.kt

**TopAppBar:**
- Use `TopAppBarDefaults.topAppBarColors(containerColor = themeColors.background, titleContentColor = themeColors.onBackground)`
- Title: `Text` with `themeColors.onBackground` + subtitle with reminder count
- Actions: Notification bell + BarChart icons with `themeColors.onBackground` tint
- Remove decorative `Icons.Default.Notifications` from `navigationIcon` slot

**Scaffold:**
- Add `containerColor = themeColors.background`

**Empty state:**
- `OutlinedCard` with `themeColors.background` container
- Icon: `Icons.Default.Notifications` with `0.3f` alpha
- Text: `0.6f` alpha primary, `0.4f` alpha secondary

**ReminderCard:**
- `Card` with `themeColors.cardBackground`, `RoundedCornerShape(16.dp)`
- Top row: Type badge chip (incomeColor/expenseColor) + amount (right-aligned)
- Title: `bodyLarge`, `FontWeight.Medium`, `themeColors.onSurface`
- Description: `bodySmall`, `themeColors.onSurface.copy(alpha = 0.6f)`
- Bottom row: Recurrence label + date, `bodySmall`, `0.5f` alpha
- Toggle: `Switch` at bottom-right
- Delete: Long-press on card shows delete confirmation dialog

### 2. AddReminderScreen.kt

**TopAppBar:**
- Use `themeColors` colors
- Back button: `Icons.AutoMirrored.Filled.ArrowBack` (consistent with other screens)

**Scaffold:**
- Add `containerColor = themeColors.background`

**Cards:**
- Use `themeColors.cardBackground` instead of `MaterialTheme.colorScheme.surfaceVariant`
- Section titles: `themeColors.primary`

**Button:**
- Use `themeColors.primary` as container color

## Files Modified

| File | Changes |
|---|---|
| `RemindersScreen.kt` | TopAppBar, cards, empty state, styling |
| `AddReminderScreen.kt` | TopAppBar, cards, button, styling |

## Verification

1. Reminders page uses themeColors throughout
2. Header shows notification + graph icons
3. Empty state matches Dashboard style
4. Cards show type badge, amount, recurrence, date
5. AddReminder matches app theme
