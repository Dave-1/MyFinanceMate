package com.deepmoneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepmoneytracker.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders ORDER BY nextTriggerTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY nextTriggerTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND nextTriggerTime <= :currentTime")
    suspend fun getDueReminders(currentTime: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
