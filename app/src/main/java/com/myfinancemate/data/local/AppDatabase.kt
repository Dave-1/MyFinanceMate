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

// Schema is exported to app/schemas/ for auto-migration support.
// For version 4+, use @AutoMigration instead of manual Migration objects:
//   @Database(..., autoMigrations = [AutoMigration(from = 3, to = 4)])
// Room will generate the migration SQL from the schema diff automatically.
// Only use manual Migration for complex transformations Room can't handle.

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
    exportSchema = true
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
                // Create accounts table for multi-account support
                db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, bankName TEXT NOT NULL, senderId TEXT NOT NULL, accountSuffix TEXT NOT NULL, isPrimary INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_senderId ON accounts(senderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isPrimary ON accounts(isPrimary)")

                // Add accountId and isSalary columns to transactions for multi-account support
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSalary INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")

                // Create fixed_expense_config table for recurring expense detection
                db.execSQL("CREATE TABLE IF NOT EXISTS fixed_expense_config (id INTEGER NOT NULL PRIMARY KEY, minOccurrences INTEGER NOT NULL, variancePercent REAL NOT NULL)")

                // Add sourceAccountId to reminders for account linking
                db.execSQL("ALTER TABLE reminders ADD COLUMN sourceAccountId INTEGER DEFAULT NULL")

                // Add ruleType to sms_rules (defaults to TRANSACTION so old rules keep working)
                db.execSQL("ALTER TABLE sms_rules ADD COLUMN ruleType TEXT NOT NULL DEFAULT 'TRANSACTION'")
            }
        }
    }
}
