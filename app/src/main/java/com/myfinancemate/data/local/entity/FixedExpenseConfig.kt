package com.myfinancemate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_expense_config")
data class FixedExpenseConfig(
    @PrimaryKey
    val id: Int = 1,
    val minOccurrences: Int = 3,
    val variancePercent: Double = 10.0
)
