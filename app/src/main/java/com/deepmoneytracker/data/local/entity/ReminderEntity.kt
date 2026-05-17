package com.deepmoneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val amount: Double? = null,
    val type: ReminderType = ReminderType.EXPENSE,
    val recurrence: Recurrence = Recurrence.NONE,
    val nextTriggerTime: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    INCOME,
    EXPENSE
}

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
