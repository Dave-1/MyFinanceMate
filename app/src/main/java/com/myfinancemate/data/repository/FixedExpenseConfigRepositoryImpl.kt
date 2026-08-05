package com.myfinancemate.data.repository

import com.myfinancemate.data.local.dao.FixedExpenseConfigDao
import com.myfinancemate.data.local.entity.FixedExpenseConfig
import com.myfinancemate.domain.repository.FixedExpenseConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixedExpenseConfigRepositoryImpl @Inject constructor(
    private val configDao: FixedExpenseConfigDao
) : FixedExpenseConfigRepository {

    override fun get(): Flow<FixedExpenseConfig?> = configDao.get()
    override suspend fun getSync(): FixedExpenseConfig? = configDao.getSync()
    override suspend fun insert(config: FixedExpenseConfig) = configDao.insert(config)
    override suspend fun update(config: FixedExpenseConfig) = configDao.update(config)
}
