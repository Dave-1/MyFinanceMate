package com.myfinancemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_rules")
data class SmsRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val senderName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
