package com.myfinancemate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myfinancemate.data.local.entity.SmsNotificationCategory
import com.myfinancemate.data.local.entity.SmsNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsNotificationDao {

    @Query("SELECT * FROM sms_notifications ORDER BY smsDate DESC")
    fun getAll(): Flow<List<SmsNotificationEntity>>

    @Query("SELECT * FROM sms_notifications WHERE category = :category ORDER BY smsDate DESC")
    fun getByCategory(category: SmsNotificationCategory): Flow<List<SmsNotificationEntity>>

    @Query("SELECT COUNT(*) FROM sms_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM sms_notifications WHERE (category = 'EXPIRY' OR category = 'RECHARGE') AND isExpired = 1 AND isRead = 0 ORDER BY smsDate DESC")
    fun getExpiredNotifications(): Flow<List<SmsNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: SmsNotificationEntity): Long

    @Delete
    suspend fun delete(notification: SmsNotificationEntity)

    @Query("DELETE FROM sms_notifications WHERE isRead = 1")
    suspend fun deleteAllRead()

    @Query("UPDATE sms_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM sms_notifications WHERE body = :body)")
    suspend fun existsByBody(body: String): Boolean

    @Query("DELETE FROM sms_notifications")
    suspend fun deleteAll()
}
