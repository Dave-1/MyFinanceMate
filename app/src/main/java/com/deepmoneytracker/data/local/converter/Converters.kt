package com.deepmoneytracker.data.local.converter

import androidx.room.TypeConverter
import com.deepmoneytracker.data.local.entity.Recurrence
import com.deepmoneytracker.data.local.entity.ReminderType
import com.deepmoneytracker.data.local.entity.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromRecurrence(value: Recurrence): String = value.name

    @TypeConverter
    fun toRecurrence(value: String): Recurrence = Recurrence.valueOf(value)
}
