package com.deepmoneytracker.domain.repository

import com.deepmoneytracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insert(category: CategoryEntity): Long
    suspend fun insertAll(categories: List<CategoryEntity>)
    suspend fun update(category: CategoryEntity)
    suspend fun delete(category: CategoryEntity)
    suspend fun deleteById(id: Long)
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun getAllCategoriesList(): List<CategoryEntity>
    suspend fun getById(id: Long): CategoryEntity?
    suspend fun getByName(name: String): CategoryEntity?
    suspend fun getCount(): Int
}
