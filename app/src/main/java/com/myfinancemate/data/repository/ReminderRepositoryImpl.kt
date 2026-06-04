package com.myfinancemate.data.repository

import com.myfinancemate.data.local.dao.ReminderDao
import com.myfinancemate.data.local.entity.ReminderEntity
import com.myfinancemate.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    override suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    override suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllReminders(): Flow<List<ReminderEntity>> = dao.getAllReminders()
    override fun getActiveReminders(): Flow<List<ReminderEntity>> = dao.getActiveReminders()
    override suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    override suspend fun getDueReminders(currentTime: Long): List<ReminderEntity> = dao.getDueReminders(currentTime)
    override suspend fun getAllRemindersList(): List<ReminderEntity> = dao.getAllRemindersList()
    override suspend fun deleteAll() = dao.deleteAll()
}
