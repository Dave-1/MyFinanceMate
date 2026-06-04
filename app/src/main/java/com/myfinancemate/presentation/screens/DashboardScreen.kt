package com.myfinancemate.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myfinancemate.presentation.theme.AppStrings
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.presentation.components.CommonTopAppBar
import com.myfinancemate.presentation.components.TopAppBarAction
import com.myfinancemate.presentation.theme.LocalThemeColors
import com.myfinancemate.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = stringResource(AppStrings.app_name),
                subtitle = stringResource(AppStrings.dashboard_subtitle),
                actions = listOf(
                    TopAppBarAction(Icons.Default.Notifications, stringResource(AppStrings.label_notifications), onNavigateToNotifications),
                    TopAppBarAction(Icons.Default.BarChart, stringResource(AppStrings.label_reports), onNavigateToReports)
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup reminder banner
            if (state.showBackupReminder) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.warning.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = themeColors.warning, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(AppStrings.backup_reminder_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                                Text(stringResource(AppStrings.backup_reminder_short), style = MaterialTheme.typography.bodySmall, color = themeColors.onBackground.copy(alpha = 0.7f))
                            }
                            IconButton(onClick = { viewModel.dismissBackupReminder() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = themeColors.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Balance Card — modern gradient style
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(AppStrings.dashboard_total_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColors.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "\u20B9${"%,.2f".format(state.balance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onPrimary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            BalanceStat(
                                label = stringResource(AppStrings.label_income),
                                amount = state.totalIncome,
                                icon = Icons.Default.ArrowUpward,
                                color = themeColors.incomeColor,
                                themeColors = themeColors
                            )
                            BalanceStat(
                                label = stringResource(AppStrings.label_expense),
                                amount = state.totalExpense,
                                icon = Icons.Default.ArrowDownward,
                                color = themeColors.expenseColor,
                                themeColors = themeColors
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(AppStrings.transactions_title),
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        onClick = onNavigateToTransactions,
                        themeColors = themeColors
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(AppStrings.reports_title),
                        icon = Icons.Default.BarChart,
                        onClick = onNavigateToReports,
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
                        stringResource(AppStrings.dashboard_recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground
                    )
                    if (state.recentTransactions.isNotEmpty()) {
                        TextButton(onClick = onNavigateToTransactions) {
                            Text(stringResource(AppStrings.dashboard_view_all), color = themeColors.primary)
                        }
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = themeColors.background)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = themeColors.onBackground.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(AppStrings.dashboard_no_transactions), style = MaterialTheme.typography.bodyLarge, color = themeColors.onBackground.copy(alpha = 0.6f))
                            Text(stringResource(AppStrings.dashboard_add_first), style = MaterialTheme.typography.bodySmall, color = themeColors.onBackground.copy(alpha = 0.4f))
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

            // Category Breakdown
            if (state.categoryTotals.isNotEmpty()) {
                item {
                    Text(
                        stringResource(AppStrings.dashboard_expense_by_category),
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
private fun BalanceStat(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    themeColors: com.myfinancemate.presentation.theme.ThemeColors
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = themeColors.onPrimary.copy(alpha = 0.7f))
            Text(
                "\u20B9${"%,.2f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themeColors.onPrimary
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    themeColors: com.myfinancemate.presentation.theme.ThemeColors
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(themeColors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = themeColors.primary, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
        }
    }
}

@Composable
private fun TransactionItem(
    description: String,
    amount: Double,
    isIncome: Boolean,
    date: Long,
    themeColors: com.myfinancemate.presentation.theme.ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(
                        if (isIncome) themeColors.incomeColor.copy(alpha = 0.12f)
                        else themeColors.expenseColor.copy(alpha = 0.12f)
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
                    Text(description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, color = themeColors.onSurface)
                    Text(
                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.onSurface.copy(alpha = 0.5f)
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
    themeColors: com.myfinancemate.presentation.theme.ThemeColors
) {
    val categoryColor = try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        themeColors.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(categoryColor))
                Column {
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = themeColors.onSurface)
                    Text("${"%.1f".format(percentage)}%", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.5f))
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
