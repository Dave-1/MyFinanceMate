package com.deepmoneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SmsRuleEntity): Long

    @Update
    suspend fun update(rule: SmsRuleEntity)

    @Delete
    suspend fun delete(rule: SmsRuleEntity)

    @Query("DELETE FROM sms_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sms_rules ORDER BY senderId ASC")
    fun getAllRules(): Flow<List<SmsRuleEntity>>

    @Query("SELECT * FROM sms_rules WHERE isActive = 1")
    suspend fun getActiveRules(): List<SmsRuleEntity>

    @Query("SELECT * FROM sms_rules WHERE senderId = :senderId LIMIT 1")
    suspend fun getBySenderId(senderId: String): SmsRuleEntity?

    @Query("SELECT * FROM sms_rules")
    suspend fun getAllRulesList(): List<SmsRuleEntity>

    @Query("DELETE FROM sms_rules")
    suspend fun deleteAll()
}
