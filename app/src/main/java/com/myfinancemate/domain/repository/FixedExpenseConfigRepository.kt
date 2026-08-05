package com.myfinancemate.domain.repository

import com.myfinancemate.data.local.entity.FixedExpenseConfig
import kotlinx.coroutines.flow.Flow

interface FixedExpenseConfigRepository {
    fun get(): Flow<FixedExpenseConfig?>
    suspend fun getSync(): FixedExpenseConfig?
    suspend fun insert(config: FixedExpenseConfig)
    suspend fun update(config: FixedExpenseConfig)
}
