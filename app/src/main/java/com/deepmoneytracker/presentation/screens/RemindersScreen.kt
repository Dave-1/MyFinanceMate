package com.deepmoneytracker.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepmoneytracker.presentation.theme.AppStrings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepmoneytracker.data.local.entity.Recurrence
import com.deepmoneytracker.data.local.entity.ReminderEntity
import com.deepmoneytracker.data.local.entity.ReminderType
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.viewmodel.ReminderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onNavigateToAdd: () -> Unit,
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    Scaffold(
        containerColor = themeColors.background,
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(AppStrings.add_reminder_title))
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(AppStrings.reminders_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(AppStrings.reminders_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = LocalThemeColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (reminder.type == ReminderType.INCOME) themeColors.incomeColor.copy(alpha = 0.15f) else themeColors.expenseColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (reminder.type == ReminderType.INCOME) "Income" else "Expense",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.type == ReminderType.INCOME) themeColors.incomeColor else themeColors.expenseColor
                    )
                }
                if (reminder.amount != null) {
                    Text(
                        text = "₹${"%,.2f".format(reminder.amount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = themeColors.onSurface
            )
            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getRecurrenceLabel(reminder.recurrence),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.primary
                    )
                    Text(
                        text = " • ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(reminder.nextTriggerTime))}",
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
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = themeColors.surface,
            titleContentColor = themeColors.onSurface,
            textContentColor = themeColors.onSurface,
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete \"${reminder.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = themeColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = themeColors.primary)
                }
            }
        )
    }
}

@Composable
private fun getRecurrenceLabel(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.NONE -> stringResource(AppStrings.recurrence_one_time)
        Recurrence.DAILY -> stringResource(AppStrings.recurrence_daily)
        Recurrence.WEEKLY -> stringResource(AppStrings.recurrence_weekly)
        Recurrence.MONTHLY -> stringResource(AppStrings.recurrence_monthly)
        Recurrence.YEARLY -> stringResource(AppStrings.recurrence_yearly)
    }
}
