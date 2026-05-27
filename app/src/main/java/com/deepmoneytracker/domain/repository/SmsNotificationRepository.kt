package com.deepmoneytracker.domain.repository

import com.deepmoneytracker.data.local.dao.SmsNotificationDao
import com.deepmoneytracker.data.local.entity.SmsNotificationCategory
import com.deepmoneytracker.data.local.entity.SmsNotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsNotificationRepository @Inject constructor(
    private val dao: SmsNotificationDao
) {
    fun getAll(): Flow<List<SmsNotificationEntity>> = dao.getAll()

    fun getByCategory(category: SmsNotificationCategory): Flow<List<SmsNotificationEntity>> = dao.getByCategory(category)

    fun getUnreadCount(): Flow<Int> = dao.getUnreadCount()

    fun getExpiredNotifications(): Flow<List<SmsNotificationEntity>> = dao.getExpiredNotifications()

    suspend fun insert(notification: SmsNotificationEntity): Long = dao.insert(notification)

    suspend fun delete(notification: SmsNotificationEntity) = dao.delete(notification)

    suspend fun deleteAllRead() = dao.deleteAllRead()

    suspend fun markAsRead(id: Long) = dao.markAsRead(id)

    suspend fun existsByBody(body: String): Boolean = dao.existsByBody(body)
}
