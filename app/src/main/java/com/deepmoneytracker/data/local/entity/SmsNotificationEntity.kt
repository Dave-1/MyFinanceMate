package com.deepmoneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SmsNotificationCategory {
    RECHARGE,
    EXPIRY,
    PROMOTION,
    OTP,
    DELIVERY,
    APPOINTMENT,
    OTHER
}

@Entity(tableName = "sms_notifications")
data class SmsNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val smsDate: Long,
    val category: SmsNotificationCategory = SmsNotificationCategory.OTHER,
    val isRead: Boolean = false,
    val isExpired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
