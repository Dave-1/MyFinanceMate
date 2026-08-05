package com.myfinancemate.domain.repository

import com.myfinancemate.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<AccountEntity>>
    suspend fun getAllList(): List<AccountEntity>
    suspend fun getPrimary(): AccountEntity?
    suspend fun getById(id: Long): AccountEntity?
    suspend fun getBySenderId(senderId: String): AccountEntity?
    suspend fun insert(account: AccountEntity): Long
    suspend fun update(account: AccountEntity)
    suspend fun delete(account: AccountEntity)
    suspend fun clearAllPrimary()
    suspend fun setPrimary(id: Long)
    suspend fun count(): Int
}
