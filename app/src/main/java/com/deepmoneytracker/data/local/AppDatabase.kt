package com.deepmoneytracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.deepmoneytracker.data.local.converter.Converters
import com.deepmoneytracker.data.local.dao.CategoryDao
import com.deepmoneytracker.data.local.dao.ReminderDao
import com.deepmoneytracker.data.local.dao.SmsRuleDao
import com.deepmoneytracker.data.local.dao.TransactionDao
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.data.local.entity.ReminderEntity
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import com.deepmoneytracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        SmsRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsRuleDao(): SmsRuleDao
}
