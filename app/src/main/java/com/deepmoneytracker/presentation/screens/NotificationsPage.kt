package com.deepmoneytracker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepmoneytracker.R
import com.deepmoneytracker.data.local.entity.SmsNotificationCategory
import com.deepmoneytracker.data.local.entity.SmsNotificationEntity
import com.deepmoneytracker.presentation.components.DateAccordionList
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.notifications_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.label_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllRead() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.notifications_clear_read),
                            tint = themeColors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background,
                    titleContentColor = themeColors.onBackground,
                    navigationIconContentColor = themeColors.onBackground
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.transactions_filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.primary.copy(alpha = 0.15f),
                            selectedLabelColor = themeColors.primary
                        )
                    )
                }
                items(SmsNotificationCategory.entries) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.setCategory(if (state.selectedCategory == category) null else category) },
                        label = { Text(getCategoryLabel(category)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getCategoryColor(category).copy(alpha = 0.15f),
                            selectedLabelColor = getCategoryColor(category)
                        )
                    )
                }
            }

            if (state.notifications.isEmpty() && state.expiredNotifications.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = themeColors.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.notifications_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = themeColors.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.notifications_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationCard(
    notification: SmsNotificationEntity,
    dateFormat: SimpleDateFormat,
    themeColors: com.deepmoneytracker.presentation.theme.ThemeColors,
    isExpired: Boolean,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(notification.category)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRead()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { it * 0.3f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !notification.isRead,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50), RoundedCornerShape(14.dp)).padding(start = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) { Text("Read", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
            }
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFE53935), RoundedCornerShape(14.dp)).padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        content = {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            expanded = !expanded
            if (!notification.isRead) onRead()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) Color(0xFFFFF3E0) else themeColors.cardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            notification.sender,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                            color = themeColors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                getCategoryLabel(notification.category),
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        dateFormat.format(Date(notification.smsDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColors.onSurface.copy(alpha = 0.5f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.isRead) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(themeColors.primary))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), modifier = Modifier.size(18.dp), tint = themeColors.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurface.copy(alpha = 0.7f),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
        }
    )
}

@Composable
private fun getCategoryLabel(category: SmsNotificationCategory): String {
    return when (category) {
        SmsNotificationCategory.RECHARGE -> stringResource(R.string.notif_recharge)
        SmsNotificationCategory.EXPIRY -> stringResource(R.string.notif_expiry)
        SmsNotificationCategory.PROMOTION -> stringResource(R.string.notif_promotion)
        SmsNotificationCategory.OTP -> stringResource(R.string.notif_otp)
        SmsNotificationCategory.DELIVERY -> stringResource(R.string.notif_delivery)
        SmsNotificationCategory.APPOINTMENT -> stringResource(R.string.notif_appointment)
        SmsNotificationCategory.OTHER -> stringResource(R.string.notif_other)
    }
}

private fun getCategoryColor(category: SmsNotificationCategory): Color {
    return when (category) {
        SmsNotificationCategory.RECHARGE -> Color(0xFF4CAF50)
        SmsNotificationCategory.EXPIRY -> Color(0xFFFF9800)
        SmsNotificationCategory.PROMOTION -> Color(0xFF9C27B0)
        SmsNotificationCategory.OTP -> Color(0xFF2196F3)
        SmsNotificationCategory.DELIVERY -> Color(0xFF00BCD4)
        SmsNotificationCategory.APPOINTMENT -> Color(0xFFE91E63)
        SmsNotificationCategory.OTHER -> Color(0xFF757575)
    }
}
