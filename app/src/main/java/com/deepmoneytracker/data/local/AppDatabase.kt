package com.deepmoneytracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.deepmoneytracker.data.local.converter.Converters
import com.deepmoneytracker.data.local.dao.CategoryDao
import com.deepmoneytracker.data.local.dao.ReminderDao
import com.deepmoneytracker.data.local.dao.SmsNotificationDao
import com.deepmoneytracker.data.local.dao.SmsRuleDao
import com.deepmoneytracker.data.local.dao.TransactionDao
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.ReminderEntity
import com.deepmoneytracker.data.local.entity.SmsNotificationEntity
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        SmsRuleEntity::class,
        SmsNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsRuleDao(): SmsRuleDao
    abstract fun smsNotificationDao(): SmsNotificationDao
}
