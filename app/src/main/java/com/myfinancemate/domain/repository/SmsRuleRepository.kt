package com.myfinancemate.domain.repository

import com.myfinancemate.data.local.entity.SmsRuleEntity
import kotlinx.coroutines.flow.Flow

interface SmsRuleRepository {
    suspend fun insert(rule: SmsRuleEntity): Long
    suspend fun update(rule: SmsRuleEntity)
    suspend fun delete(rule: SmsRuleEntity)
    suspend fun deleteById(id: Long)
    fun getAllRules(): Flow<List<SmsRuleEntity>>
    suspend fun getActiveRules(): List<SmsRuleEntity>
    suspend fun getBySenderId(senderId: String): SmsRuleEntity?
    suspend fun getAllRulesList(): List<SmsRuleEntity>
    suspend fun getCount(): Int
    suspend fun insertAll(rules: List<SmsRuleEntity>)
    suspend fun deleteAll()
}
