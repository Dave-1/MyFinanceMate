package com.myfinancemate.data.repository

import com.myfinancemate.data.local.dao.AccountDao
import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAll(): Flow<List<AccountEntity>> = accountDao.getAll()
    override suspend fun getAllList(): List<AccountEntity> = accountDao.getAllList()
    override suspend fun getPrimary(): AccountEntity? = accountDao.getPrimary()
    override suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)
    override suspend fun getBySenderId(senderId: String): AccountEntity? = accountDao.getBySenderId(senderId)
    override suspend fun insert(account: AccountEntity): Long = accountDao.insert(account)
    override suspend fun update(account: AccountEntity) = accountDao.update(account)
    override suspend fun delete(account: AccountEntity) = accountDao.delete(account)
    override suspend fun clearAllPrimary() = accountDao.clearAllPrimary()
    override suspend fun count(): Int = accountDao.count()

    override suspend fun setPrimary(id: Long) {
        accountDao.clearAllPrimary()
        accountDao.getById(id)?.let { account ->
            accountDao.update(account.copy(isPrimary = true))
        }
    }
}
