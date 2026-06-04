package com.myfinancemate.data.repository

import com.myfinancemate.data.local.dao.SmsRuleDao
import com.myfinancemate.data.local.entity.SmsRuleEntity
import com.myfinancemate.domain.repository.SmsRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRuleRepositoryImpl @Inject constructor(
    private val dao: SmsRuleDao
) : SmsRuleRepository {

    override suspend fun insert(rule: SmsRuleEntity): Long = dao.insert(rule)
    override suspend fun update(rule: SmsRuleEntity) = dao.update(rule)
    override suspend fun delete(rule: SmsRuleEntity) = dao.delete(rule)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllRules(): Flow<List<SmsRuleEntity>> = dao.getAllRules()
    override suspend fun getActiveRules(): List<SmsRuleEntity> = dao.getActiveRules()
    override suspend fun getBySenderId(senderId: String): SmsRuleEntity? = dao.getBySenderId(senderId)
    override suspend fun getAllRulesList(): List<SmsRuleEntity> = dao.getAllRulesList()
    override suspend fun deleteAll() = dao.deleteAll()
}
