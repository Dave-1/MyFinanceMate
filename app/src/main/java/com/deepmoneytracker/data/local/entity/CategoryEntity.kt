package com.deepmoneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "ic_category",
    val color: String = "#6750A4",
    val isDefault: Boolean = false,
    val keywords: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
