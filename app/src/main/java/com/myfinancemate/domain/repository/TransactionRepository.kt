package com.myfinancemate.domain.repository

import com.myfinancemate.data.local.dao.CategoryTotal
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: TransactionEntity): Long
    suspend fun update(transaction: TransactionEntity)
    suspend fun delete(transaction: TransactionEntity)
    suspend fun existsBySmsBody(smsBody: String): Boolean
    suspend fun deleteById(id: Long)
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun getById(id: Long): TransactionEntity?
    fun getByIdFlow(id: Long): Flow<TransactionEntity?>
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>
    fun getByCategoryId(categoryId: Long): Flow<List<TransactionEntity>>
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getByDateRangeAndType(startDate: Long, endDate: Long, type: TransactionType): Flow<List<TransactionEntity>>
    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double?>
    fun getTotalByType(type: TransactionType): Flow<Double?>
    fun getCategoryWiseTotal(type: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
    fun search(query: String): Flow<List<TransactionEntity>>
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>
    suspend fun getAllTransactionsList(): List<TransactionEntity>
    suspend fun deleteAll()
}
