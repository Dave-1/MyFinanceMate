# Date Accordion Grouping — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add date-grouped collapsible accordions to every transaction view (All, Income, Expense, SMS, ByBank) with contextual summaries on the right side of each header.

**Architecture:** Extract a reusable `DateAccordionList` composable and a shared `getDateHeader()` utility. The existing SMS tab already has accordion logic — we generalize it. Apply to All/Income/Expense (transaction amount + count), ByBank (Bank > Date nesting), and keep SMS tab's existing accordion with count-only summary.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room (no data layer changes needed)

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `presentation/components/DateUtils.kt` | **Create** | Shared `getDateHeader()` utility |
| `presentation/components/DateAccordionList.kt` | **Create** | Reusable date accordion composable |
| `presentation/screens/TransactionsScreen.kt` | **Modify** | Apply accordions to All/Income/Expense, refactor ByBank to Bank>Date, remove duplicate getDateHeader |

---

### Task 1: Create shared `DateUtils.kt`

**Files:**
- Create: `app/src/main/java/com/deepmoneytracker/presentation/components/DateUtils.kt`

- [ ] **Step 1: Create the file with `getDateHeader` function**

```kotlin
package com.deepmoneytracker.presentation.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Returns "Today", "Yesterday", or "dd MMM yyyy" for the given timestamp.
 */
fun getDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = cal.timeInMillis

    val itemCal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val itemDay = itemCal.timeInMillis

    return when {
        itemDay == today -> "Today"
        itemDay == yesterday -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Groups items by date header, sorted descending by timestamp.
 */
fun <T> List<T>.groupByDateHeader(getDate: (T) -> Long): Map<String, List<T>> {
    return sortedByDescending { getDate(it) }
        .groupBy { getDateHeader(getDate(it)) }
}

/**
 * Formats a currency amount with sign. e.g. "+₹2,450" or "-₹180"
 */
fun formatAmountWithSign(amount: Double, isPositive: Boolean): String {
    val formatted = "₹%,.0f".format(amount)
    return if (isPositive) "+$formatted" else "-$formatted"
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/components/DateUtils.kt
git commit -m "feat: add shared DateUtils with getDateHeader and groupByDateHeader"
```

---

### Task 2: Create `DateAccordionList` composable

**Files:**
- Create: `app/src/main/java/com/deepmoneytracker/presentation/components/DateAccordionList.kt`

- [ ] **Step 1: Create the reusable accordion composable**

```kotlin
package com.deepmoneytracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.theme.ThemeColors

/**
 * A reusable date-grouped accordion list.
 *
 * @param items The flat list of items to group by date
 * @param getDate Extracts the timestamp from each item for grouping
 * @param summaryRight Returns the right-side text for a date header (e.g. "₹2,450 (3)" or "5 SMS")
 * @param itemKey Returns a unique key for each item (for LazyColumn)
 * @param itemContent Composable to render each individual item
 * @param extraContent Optional extra LazyListScope content appended after all date groups (e.g. "Manual Entries" section)
 * @param autoExpandFirst Whether to auto-expand the most recent date group
 * @param footerContent Optional composable rendered at the bottom of the list
 */
@Composable
fun <T> DateAccordionList(
    items: List<T>,
    getDate: (T) -> Long,
    summaryRight: (List<T>) -> String,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    themeColors: ThemeColors = LocalThemeColors.current,
    autoExpandFirst: Boolean = true,
    extraContent: (LazyListScope.() -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null
) {
    val groupedByDate = items.groupByDateHeader(getDate)
    var expandedDates by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(groupedByDate.keys) {
        if (autoExpandFirst && groupedByDate.isNotEmpty() && expandedDates.isEmpty()) {
            expandedDates = setOf(groupedByDate.keys.first())
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedByDate.forEach { (dateHeader, dateItems) ->
            val isExpanded = dateHeader in expandedDates

            item(key = "header_$dateHeader") {
                DateAccordionHeader(
                    dateHeader = dateHeader,
                    summary = summaryRight(dateItems),
                    isExpanded = isExpanded,
                    themeColors = themeColors,
                    onClick = {
                        expandedDates = if (isExpanded) {
                            expandedDates - dateHeader
                        } else {
                            expandedDates + dateHeader
                        }
                    }
                )
            }

            if (isExpanded) {
                items(dateItems, key = { itemKey(it) }) { item ->
                    itemContent(item)
                }
            }
        }

        extraContent?.invoke(this)

        if (footerContent != null) {
            item { footerContent() }
        }
    }
}

@Composable
fun DateAccordionHeader(
    dateHeader: String,
    summary: String,
    isExpanded: Boolean,
    themeColors: ThemeColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.primary.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = themeColors.primary
                )
                Text(
                    dateHeader,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
            }
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/components/DateAccordionList.kt
git commit -m "feat: add reusable DateAccordionList composable"
```

---

### Task 3: Apply date accordions to All/Income/Expense views

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt`

The `else` branch (line ~456-474) currently renders a flat `LazyColumn` for All, Income, and Expense views. Replace it with `DateAccordionList`.

Also replace the SMS tab's inline accordion (lines ~253-332) with `DateAccordionList` to reuse the shared component.

- [ ] **Step 1: Add imports for the new components**

Add at the top of TransactionsScreen.kt, after existing imports:

```kotlin
import com.deepmoneytracker.presentation.components.DateAccordionList
import com.deepmoneytracker.presentation.components.formatAmountWithSign
```

- [ ] **Step 2: Replace the flat transaction list (else branch) with DateAccordionList**

Replace the `else` block (lines ~456-474):

```kotlin
            } else {
                DateAccordionList(
                    items = filteredTransactions,
                    getDate = { it.date },
                    summaryRight = { txns ->
                        val totalIncome = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val totalExpense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val net = totalIncome - totalExpense
                        val sign = if (net >= 0) "+" else ""
                        "${sign}₹%,.0f".format(net) + " (${txns.size})"
                    },
                    itemKey = { it.id },
                    itemContent = { transaction ->
                        TransactionCard(
                            description = transaction.description,
                            merchant = transaction.merchant,
                            amount = transaction.amount,
                            isIncome = transaction.type == TransactionType.INCOME,
                            date = transaction.date,
                            isFromSms = transaction.isFromSms,
                            onClick = { onNavigateToEdit(transaction.id) },
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            themeColors = themeColors
                        )
                    },
                    themeColors = themeColors,
                    footerContent = { Spacer(modifier = Modifier.height(80.dp)) }
                )
            }
```

- [ ] **Step 3: Replace SMS tab's inline accordion with DateAccordionList**

Replace the SMS accordion section (lines ~253-332, the `LazyColumn` with `groupedByDate.forEach`) with:

```kotlin
                    DateAccordionList(
                        items = (expired + notifications).sortedByDescending { it.smsDate },
                        getDate = { it.smsDate },
                        summaryRight = { smsList -> "${smsList.size} SMS" },
                        itemKey = { it.id },
                        itemContent = { notification ->
                            SmsNotificationCard(
                                notification, dateFormat, themeColors,
                                isExpired = notification.isExpired && !notification.isRead,
                                onRead = { notificationsViewModel.markAsRead(notification.id) },
                                onDelete = { notificationsViewModel.deleteNotification(notification) }
                            )
                        },
                        themeColors = themeColors,
                        footerContent = { Spacer(modifier = Modifier.height(80.dp)) }
                    )
```

- [ ] **Step 4: Remove the old local `getDateHeader` composable**

Delete the `getDateHeader` function (lines ~385-410 area) and any related imports that are no longer used. The shared `getDateHeader` from `DateUtils.kt` replaces it.

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git commit -m "feat: add date accordions to All/Income/Expense/SMS views"
```

---

### Task 4: Apply Bank > Date accordions to ByBank view

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt`

The ByBank view (lines ~369-455) currently groups by bank with a flat list inside each bank. Change it to show date accordions nested inside each bank group.

- [ ] **Step 1: Replace ByBank section with Bank > Date nesting**

Replace the `selectedFilter == "ByBank"` block with:

```kotlin
            } else if (selectedFilter == "ByBank") {
                // Group by bank, then date within each bank
                val grouped = filteredTransactions.filter { it.senderInfo.isNotBlank() }.groupBy { it.senderInfo }
                val nonBank = filteredTransactions.filter { it.senderInfo.isBlank() }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grouped.forEach { (bank, txns) ->
                        // Bank header
                        val totalAmount = txns.sumOf {
                            if (it.type == TransactionType.INCOME) it.amount else -it.amount
                        }
                        val sign = if (totalAmount >= 0) "+" else ""
                        item(key = "header_$bank") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = themeColors.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        bank,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.primary
                                    )
                                    Text(
                                        "${sign}₹%,.0f".format(totalAmount) + " (${txns.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Date accordions within this bank
                        val bankGroupedByDate = txns.groupByDateHeader { it.date }
                        var expandedDates by remember { mutableStateOf(setOf<String>()) }

                        // Auto-expand first date
                        LaunchedEffect(bankGroupedByDate.keys) {
                            if (bankGroupedByDate.isNotEmpty() && expandedDates.isEmpty()) {
                                expandedDates = setOf(bankGroupedByDate.keys.first())
                            }
                        }

                        bankGroupedByDate.forEach { (dateHeader, dateTxns) ->
                            val isExpanded = dateHeader in expandedDates
                            val dateTotal = dateTxns.sumOf {
                                if (it.type == TransactionType.INCOME) it.amount else -it.amount
                            }
                            val dateSign = if (dateTotal >= 0) "+" else ""

                            item(key = "date_${bank}_$dateHeader") {
                                DateAccordionHeader(
                                    dateHeader = dateHeader,
                                    summary = "${dateSign}₹%,.0f".format(dateTotal) + " (${dateTxns.size})",
                                    isExpanded = isExpanded,
                                    themeColors = themeColors,
                                    onClick = {
                                        expandedDates = if (isExpanded) {
                                            expandedDates - dateHeader
                                        } else {
                                            expandedDates + dateHeader
                                        }
                                    }
                                )
                            }

                            if (isExpanded) {
                                items(dateTxns, key = { it.id }) { transaction ->
                                    TransactionCard(
                                        description = transaction.description,
                                        merchant = transaction.merchant,
                                        amount = transaction.amount,
                                        isIncome = transaction.type == TransactionType.INCOME,
                                        date = transaction.date,
                                        isFromSms = transaction.isFromSms,
                                        onClick = { onNavigateToEdit(transaction.id) },
                                        onDelete = { viewModel.deleteTransaction(transaction) },
                                        themeColors = themeColors
                                    )
                                }
                            }
                        }
                    }
                    // Non-bank transactions
                    if (nonBank.isNotEmpty()) {
                        item(key = "header_manual") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = themeColors.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Manual Entries",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.primary
                                    )
                                    val manualTotal = nonBank.sumOf {
                                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                                    }
                                    val manualSign = if (manualTotal >= 0) "+" else ""
                                    Text(
                                        "${manualSign}₹%,.0f".format(manualTotal) + " (${nonBank.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        items(nonBank, key = { it.id }) { transaction ->
                            TransactionCard(
                                description = transaction.description,
                                merchant = transaction.merchant,
                                amount = transaction.amount,
                                isIncome = transaction.type == TransactionType.INCOME,
                                date = transaction.date,
                                isFromSms = transaction.isFromSms,
                                onClick = { onNavigateToEdit(transaction.id) },
                                onDelete = { viewModel.deleteTransaction(transaction) },
                                themeColors = themeColors
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
```

- [ ] **Step 2: Add missing import for `DateAccordionHeader`**

Add import at top of file:

```kotlin
import com.deepmoneytracker.presentation.components.DateAccordionHeader
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/TransactionsScreen.kt
git commit -m "feat: add Bank > Date accordions to ByBank view"
```

---

### Task 5: Clean up NotificationsPage.kt duplicate getDateHeader

**Files:**
- Modify: `app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt`

The NotificationsPage.kt has its own inline `getDateHeader` and inline accordion logic. Replace with the shared `DateAccordionList`.

- [ ] **Step 1: Add imports**

```kotlin
import com.deepmoneytracker.presentation.components.DateAccordionList
```

- [ ] **Step 2: Replace the inline accordion in NotificationsPage with DateAccordionList**

Replace the `LazyColumn` block (lines ~172-253) that does `groupedByDate.forEach` with:

```kotlin
                DateAccordionList(
                    items = (state.expiredNotifications + state.notifications).sortedByDescending { it.smsDate },
                    getDate = { it.smsDate },
                    summaryRight = { smsList -> "${smsList.size} SMS" },
                    itemKey = { "sms_${it.id}" },
                    itemContent = { notification ->
                        NotificationCard(
                            notification, dateFormat, themeColors,
                            isExpired = notification.isExpired && !notification.isRead,
                            onRead = { viewModel.markAsRead(notification.id) },
                            onDelete = { viewModel.deleteNotification(notification) }
                        )
                    },
                    themeColors = themeColors
                )
```

- [ ] **Step 3: Delete the local `getDateHeader` function from NotificationsPage.kt**

Remove the `getDateHeader` composable function (lines ~384-410).

- [ ] **Step 4: Remove unused imports**

Remove any imports that were only used by the deleted code (e.g. `SimpleDateFormat`, `Date` from `java.util` if no longer used).

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/deepmoneytracker/presentation/screens/NotificationsPage.kt
git commit -m "refactor: use shared DateAccordionList in NotificationsPage"
```

---

### Task 6: Final verification

- [ ] **Step 1: Full build**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit any remaining changes**

```bash
git status
```

If there are uncommitted changes, commit them.
