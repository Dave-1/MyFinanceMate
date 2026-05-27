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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Deep Money Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onBackground
                        )
                        Text(
                            "Your finances at a glance",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = themeColors.primary
                        )
                    }
                    IconButton(onClick = onNavigateToReports) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Reports",
                            tint = themeColors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background,
                    titleContentColor = themeColors.onBackground
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Backup reminder banner
            if (state.showBackupReminder) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.warning.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = themeColors.warning,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "SMS Backup Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onBackground
                                )
                                Text(
                                    "It's been a while since your last SMS backup. Back up now to keep your data safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.dismissBackupReminder() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = themeColors.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total Balance",
                            style = MaterialTheme.typography.titleSmall,
                            color = themeColors.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "\u20B9${"%,.2f".format(state.balance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onPrimary
                        )
                    }
                }
            }

            // Income & Expense Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Income",
                        amount = state.totalIncome,
                        icon = Icons.Default.ArrowUpward,
                        color = themeColors.incomeColor,
                        themeColors = themeColors
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Expense",
                        amount = state.totalExpense,
                        icon = Icons.Default.ArrowDownward,
                        color = themeColors.expenseColor,
                        themeColors = themeColors
                    )
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground
                    )
                    if (state.recentTransactions.isNotEmpty()) {
                        IconButton(onClick = onNavigateToTransactions) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "View All",
                                tint = themeColors.primary
                            )
                        }
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = themeColors.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = themeColors.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "Add your first transaction",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(state.recentTransactions.take(5)) { transaction ->
                    TransactionItem(
                        description = transaction.description,
                        amount = transaction.amount,
                        isIncome = transaction.type == TransactionType.INCOME,
                        date = transaction.date,
                        themeColors = themeColors
                    )
                }
            }

            // Category Breakdown Header
            if (state.categoryTotals.isNotEmpty()) {
                item {
                    Text(
                        "Expense by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground
                    )
                }

                items(state.categoryTotals.take(5)) { ct ->
                    CategoryBreakdownItem(
                        name = ct.category?.name ?: "Unknown",
                        total = ct.total,
                        percentage = ct.percentage,
                        color = ct.category?.color ?: "#9E9E9E",
                        themeColors = themeColors
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "\u20B9${"%,.2f".format(amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )
        }
    }
}

@Composable
private fun TransactionItem(
    description: String,
    amount: Double,
    isIncome: Boolean,
    date: Long,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(20.dp)
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
                    Text(
                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                "${if (isIncome) "+" else "-"}\u20B9${"%,.2f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) themeColors.incomeColor else themeColors.expenseColor
            )
        }
    }
}

@Composable
private fun CategoryBreakdownItem(
    name: String,
    total: Double,
    percentage: Float,
    color: String,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors
) {
    val categoryColor = try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        themeColors.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = themeColors.onSurface
                    )
                    Text(
                        "${"%.1f".format(percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                "\u20B9${"%,.2f".format(total)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themeColors.expenseColor
            )
        }
    }
}
