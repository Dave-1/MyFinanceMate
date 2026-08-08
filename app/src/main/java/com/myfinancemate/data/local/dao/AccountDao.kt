package com.myfinancemate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.myfinancemate.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isPrimary DESC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isPrimary DESC, name ASC")
    suspend fun getAllList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE senderId = :senderId LIMIT 1")
    suspend fun getBySenderId(senderId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("UPDATE accounts SET isPrimary = 0")
    suspend fun clearAllPrimary()

    @Transaction
    suspend fun setPrimary(id: Long) {
        clearAllPrimary()
        getById(id)?.let { update(it.copy(isPrimary = true)) }
    }

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
