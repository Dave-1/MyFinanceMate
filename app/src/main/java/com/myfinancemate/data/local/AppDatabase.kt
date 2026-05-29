package com.myfinancemate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myfinancemate.data.local.converter.Converters
import com.myfinancemate.data.local.dao.CategoryDao
import com.myfinancemate.data.local.dao.ReminderDao
import com.myfinancemate.data.local.dao.SmsNotificationDao
import com.myfinancemate.data.local.dao.SmsRuleDao
import com.myfinancemate.data.local.dao.TransactionDao
import com.myfinancemate.data.local.entity.CategoryEntity
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.data.local.entity.TransactionEntity

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
