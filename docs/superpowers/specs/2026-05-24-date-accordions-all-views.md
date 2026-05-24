# Date Accordion Grouping for All Transaction Views

## Overview
Add date-grouped collapsible accordions to every transaction view (All, Income, Expense, SMS, ByBank). Each accordion header shows the date on the left and contextual summary on the right.

## Accordion Header Layout

### Transaction Views (All, Income, Expense)
```
| 22 May 2026 |           ₹2,450 (3) |
| Today       |        -₹1,200 (2)    |
```
- **Left**: Date label ("Today", "Yesterday", or "dd MMM yyyy")
- **Right**: Net amount (income green, expense red, net shows dominant color) + transaction count in parentheses

### SMS Notifications View
```
| 22 May 2026 |              5 SMS    |
| Today       |              2 SMS    |
```
- **Left**: Date label
- **Right**: SMS count only

### ByBank View
Nested: Bank > Date
```
| HDFC Bank                         |
|   | Today       |    ₹2,450 (3)   |
|   | Yesterday   |      ₹180 (1)   |
| SBI                               |
|   | 22 May 2026 |   ₹3,200 (5)    |
```
- Bank header: bank name left, total amount + count right
- Date header inside: date left, amount + count right

## Behavior
- Most recent date group auto-expanded on first load
- Click header to toggle expand/collapse
- Income amounts shown in green, expenses in red
- Net amount per date: sum of income minus expenses; color = dominant type
- Search results maintain accordion grouping
- Swipe-to-delete on individual items still works

## Shared Component
Extract `DateAccordionList` composable used by all views. The component accepts:
- A pre-grouped map of `String (date header)` → `List<Item>`
- Right-side summary renderer lambda (amount+count vs count-only)
- Item renderer lambda (TransactionCard vs SmsNotificationCard)

## Files to Modify
1. **TransactionsScreen.kt** — Major refactor: extract `DateAccordionList`, apply to all branches
2. **TransactionViewModel.kt** — Ensure data is sorted by date DESC (already is via DAO)
3. **NotificationsViewModel.kt** — No changes needed (data already sorted)

## String Resources
- Reuse existing: `R.string.transactions_title`, date formatting
- No new strings needed (counts are dynamic)
