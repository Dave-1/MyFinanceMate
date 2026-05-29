package com.myfinancemate.domain.repository

import com.myfinancemate.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    suspend fun insert(reminder: ReminderEntity): Long
    suspend fun update(reminder: ReminderEntity)
    suspend fun delete(reminder: ReminderEntity)
    suspend fun deleteById(id: Long)
    fun getAllReminders(): Flow<List<ReminderEntity>>
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    suspend fun getById(id: Long): ReminderEntity?
    suspend fun getDueReminders(currentTime: Long): List<ReminderEntity>
    suspend fun getAllRemindersList(): List<ReminderEntity>
    suspend fun deleteAll()
}
