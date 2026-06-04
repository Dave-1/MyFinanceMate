package com.myfinancemate.data.repository

import com.myfinancemate.data.local.dao.CategoryTotal
import com.myfinancemate.data.local.dao.TransactionDao
import com.myfinancemate.data.local.entity.TransactionEntity
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction)
    override suspend fun update(transaction: TransactionEntity) = dao.update(transaction)
    override suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)
    override suspend fun existsBySmsBody(smsBody: String): Boolean = dao.existsBySmsBody(smsBody)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactions()
    override suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)
    override fun getByIdFlow(id: Long): Flow<TransactionEntity?> = dao.getByIdFlow(id)
    override fun getByType(type: TransactionType): Flow<List<TransactionEntity>> = dao.getByType(type)
    override fun getByCategoryId(categoryId: Long): Flow<List<TransactionEntity>> = dao.getByCategoryId(categoryId)
    override fun getByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = dao.getByDateRange(startDate, endDate)
    override fun getByDateRangeAndType(startDate: Long, endDate: Long, type: TransactionType): Flow<List<TransactionEntity>> = dao.getByDateRangeAndType(startDate, endDate, type)
    override fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): Flow<Double?> = dao.getTotalByTypeAndDateRange(type, startDate, endDate)
    override fun getTotalByType(type: TransactionType): Flow<Double?> = dao.getTotalByType(type)
    override fun getCategoryWiseTotal(type: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> = dao.getCategoryWiseTotal(type, startDate, endDate)
    override fun search(query: String): Flow<List<TransactionEntity>> = dao.search(query)
    override fun getRecent(limit: Int): Flow<List<TransactionEntity>> = dao.getRecent(limit)
    override suspend fun getAllTransactionsList(): List<TransactionEntity> = dao.getAllTransactionsList()
    override suspend fun deleteAll() = dao.deleteAll()
}
