package com.deepmoneytracker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.presentation.components.DateAccordionList
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bankNames by viewModel.bankNames.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    var selectedFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredTransactions = when (selectedFilter) {
        "Income" -> transactions.filter { it.type == TransactionType.INCOME }
        "Expense" -> transactions.filter { it.type == TransactionType.EXPENSE }
        "SMS" -> transactions.filter { it.isFromSms }
        null -> transactions
        else -> {
            // Bank name filter
            transactions.filter { it.senderInfo.contains(selectedFilter ?: "", ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background,
                    titleContentColor = themeColors.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = themeColors.primary,
                contentColor = themeColors.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transactions...", color = themeColors.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = themeColors.onSurface.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = themeColors.cardBackground,
                    unfocusedContainerColor = themeColors.cardBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipCustom(
                        label = "All",
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        themeColors = themeColors
                    )
                }
                item {
                    FilterChipCustom(
                        label = "Income",
                        selected = selectedFilter == "Income",
                        onClick = { selectedFilter = if (selectedFilter == "Income") null else "Income" },
                        themeColors = themeColors
                    )
                }
                item {
                    FilterChipCustom(
                        label = "Expense",
                        selected = selectedFilter == "Expense",
                        onClick = { selectedFilter = if (selectedFilter == "Expense") null else "Expense" },
                        themeColors = themeColors
                    )
                }
                item {
                    FilterChipCustom(
                        label = "SMS",
                        selected = selectedFilter == "SMS",
                        onClick = { selectedFilter = if (selectedFilter == "SMS") null else "SMS" },
                        themeColors = themeColors
                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction list or empty state
            if (filteredTransactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(themeColors.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = themeColors.primary.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No transactions yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = themeColors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap + to add your first transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface.copy(alpha = 0.6f)
                    )
                }
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
        }
    }
}

@Composable
private fun FilterChipCustom(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) themeColors.primary else themeColors.cardBackground,
        contentColor = if (selected) themeColors.onPrimary else themeColors.onSurface,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TransactionCard(
    description: String,
    merchant: String,
    amount: Double,
    isIncome: Boolean,
    date: Long,
    isFromSms: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with colored background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) themeColors.incomeColor.copy(alpha = 0.15f)
                            else themeColors.expenseColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isIncome) themeColors.incomeColor else themeColors.expenseColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = themeColors.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (merchant.isNotBlank()) {
                            Text(
                                merchant,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                            Text(
                                "\u2022",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        Text(
                            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface.copy(alpha = 0.6f)
                        )
                        if (isFromSms) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = "From SMS",
                                modifier = Modifier.size(14.dp),
                                tint = themeColors.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Amount and delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}\u20B9${"%,.2f".format(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) themeColors.incomeColor else themeColors.expenseColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = themeColors.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
