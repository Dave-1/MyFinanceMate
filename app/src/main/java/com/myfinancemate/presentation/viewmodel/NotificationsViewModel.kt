package com.myfinancemate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myfinancemate.data.local.entity.SmsNotificationCategory
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import com.myfinancemate.domain.repository.SmsNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Categories shown in Notifications — excludes OTP, PROMOTION, DELIVERY (informational only)
private val NOTIFICATION_CATEGORIES = setOf(
    SmsNotificationCategory.RECHARGE,
    SmsNotificationCategory.EXPIRY,
    SmsNotificationCategory.APPOINTMENT,
    SmsNotificationCategory.OTHER
)

data class NotificationsState(
    val notifications: List<SmsNotificationEntity> = emptyList(),
    val expiredNotifications: List<SmsNotificationEntity> = emptyList(),
    val selectedCategory: SmsNotificationCategory? = null,
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val smsNotificationRepository: SmsNotificationRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<SmsNotificationCategory?>(null)

    val state: StateFlow<NotificationsState> = combine(
        smsNotificationRepository.getAll(),
        smsNotificationRepository.getUnreadCount(),
        selectedCategory
    ) { allNotifications, unreadCount, filter ->
        // Filter to actionable notification categories only (no OTP, PROMOTION, DELIVERY)
        val actionable = allNotifications.filter { it.category in NOTIFICATION_CATEGORIES }

        // Split into expired and regular
        val allExpired = actionable.filter { it.isExpired && !it.isRead }
        val allRegular = actionable.filter { !(it.isExpired && !it.isRead) }

        // Apply category filter to BOTH lists
        val expired = if (filter != null) {
            allExpired.filter { it.category == filter }
        } else {
            allExpired
        }

        val regular = if (filter != null) {
            allRegular.filter { it.category == filter }
        } else {
            allRegular
        }

        NotificationsState(
            notifications = regular,
            expiredNotifications = expired,
            selectedCategory = filter,
            unreadCount = unreadCount,
            totalCount = regular.size + expired.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationsState()
    )

    fun setCategory(category: SmsNotificationCategory?) {
        selectedCategory.value = category
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            smsNotificationRepository.markAsRead(id)
        }
    }

    fun deleteNotification(notification: SmsNotificationEntity) {
        viewModelScope.launch {
            smsNotificationRepository.delete(notification)
        }
    }

    fun clearAllRead() {
        viewModelScope.launch {
            smsNotificationRepository.deleteAllRead()
        }
    }
}
