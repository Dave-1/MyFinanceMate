package com.myfinancemate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myfinancemate.data.local.entity.FixedExpenseConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseConfigDao {
    @Query("SELECT * FROM fixed_expense_config WHERE id = 1")
    fun get(): Flow<FixedExpenseConfig?>

    @Query("SELECT * FROM fixed_expense_config WHERE id = 1")
    suspend fun getSync(): FixedExpenseConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: FixedExpenseConfig)

    @Update
    suspend fun update(config: FixedExpenseConfig)
}
