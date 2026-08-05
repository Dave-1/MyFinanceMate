package com.myfinancemate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myfinancemate.data.local.converter.Converters
import com.myfinancemate.data.local.dao.AccountDao
import com.myfinancemate.data.local.dao.CategoryDao
import com.myfinancemate.data.local.dao.FixedExpenseConfigDao
import com.myfinancemate.data.local.dao.ReminderDao
import com.myfinancemate.data.local.dao.SmsNotificationDao
import com.myfinancemate.data.local.dao.SmsRuleDao
import com.myfinancemate.data.local.dao.TransactionDao
import com.myfinancemate.data.local.entity.CategoryEntity
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.data.local.entity.FixedExpenseConfig
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        ReminderEntity::class,
        SmsRuleEntity::class,
        SmsNotificationEntity::class,
        AccountEntity::class,
        FixedExpenseConfig::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsRuleDao(): SmsRuleDao
    abstract fun smsNotificationDao(): SmsNotificationDao
    abstract fun accountDao(): AccountDao
    abstract fun fixedExpenseConfigDao(): FixedExpenseConfigDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, bankName TEXT NOT NULL DEFAULT '', senderId TEXT NOT NULL, accountSuffix TEXT NOT NULL DEFAULT '', isPrimary INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_senderId ON accounts(senderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounts_isPrimary ON accounts(isPrimary)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSalary INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS fixed_expense_config (id INTEGER PRIMARY KEY, minOccurrences INTEGER NOT NULL DEFAULT 3, variancePercent REAL NOT NULL DEFAULT 10.0)")
                db.execSQL("ALTER TABLE reminders ADD COLUMN sourceAccountId INTEGER DEFAULT NULL")
                // SmsRuleEntity gained a ruleType column. Not in the prompt's SQL block,
                // but required so existing sms_rules rows match the new entity.
                // Defaults to TRANSACTION so old rules keep working.
                db.execSQL("ALTER TABLE sms_rules ADD COLUMN ruleType TEXT NOT NULL DEFAULT 'TRANSACTION'")
            }
        }
    }
}
