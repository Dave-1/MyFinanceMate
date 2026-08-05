package com.myfinancemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SmsRuleType { TRANSACTION, SALARY, FIXED_EXPENSE }

@Entity(tableName = "sms_rules")
data class SmsRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val senderName: String = "",
    val isActive: Boolean = true,
    val ruleType: SmsRuleType = SmsRuleType.TRANSACTION,
    val createdAt: Long = System.currentTimeMillis()
)
