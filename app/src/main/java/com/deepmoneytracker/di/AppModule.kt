package com.deepmoneytracker.di

import android.content.Context
import androidx.room.Room
import com.deepmoneytracker.data.local.AppDatabase
import com.deepmoneytracker.data.local.dao.CategoryDao
import com.deepmoneytracker.data.local.dao.ReminderDao
import com.deepmoneytracker.data.local.dao.SmsRuleDao
import com.deepmoneytracker.data.local.dao.TransactionDao
import com.deepmoneytracker.data.repository.CategoryRepositoryImpl
import com.deepmoneytracker.data.repository.ReminderRepositoryImpl
import com.deepmoneytracker.data.repository.SmsRuleRepositoryImpl
import com.deepmoneytracker.data.repository.TransactionRepositoryImpl
import com.deepmoneytracker.domain.repository.CategoryRepository
import com.deepmoneytracker.domain.repository.ReminderRepository
import com.deepmoneytracker.domain.repository.SmsRuleRepository
import com.deepmoneytracker.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "deep_money_tracker.db"
        ).build()
    }

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideSmsRuleDao(db: AppDatabase): SmsRuleDao = db.smsRuleDao()

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository =
        TransactionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(dao: CategoryDao): CategoryRepository =
        CategoryRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository =
        ReminderRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideSmsRuleRepository(dao: SmsRuleDao): SmsRuleRepository =
        SmsRuleRepositoryImpl(dao)
}
