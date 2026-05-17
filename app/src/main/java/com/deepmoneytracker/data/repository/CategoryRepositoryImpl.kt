package com.deepmoneytracker.data.repository

import com.deepmoneytracker.data.local.dao.CategoryDao
import com.deepmoneytracker.data.local.entity.CategoryEntity
import com.deepmoneytracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override suspend fun insert(category: CategoryEntity): Long = dao.insert(category)
    override suspend fun insertAll(categories: List<CategoryEntity>) = dao.insertAll(categories)
    override suspend fun update(category: CategoryEntity) = dao.update(category)
    override suspend fun delete(category: CategoryEntity) = dao.delete(category)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllCategories(): Flow<List<CategoryEntity>> = dao.getAllCategories()
    override suspend fun getAllCategoriesList(): List<CategoryEntity> = dao.getAllCategoriesList()
    override suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)
    override suspend fun getByName(name: String): CategoryEntity? = dao.getByName(name)
    override suspend fun getCount(): Int = dao.getCount()
}
