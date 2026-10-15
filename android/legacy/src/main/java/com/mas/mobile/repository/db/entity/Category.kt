package com.mas.mobile.repository.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], name = "index_categories_on_name"),
        Index(value = ["display_order", "name"], name = "index_categories_on_order_and_name")
    ]
)
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "plan")
    val plan: Double,

    @ColumnInfo(name = "active")
    val isActive: Boolean,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "merchants")
    val merchants: List<String>,

    @ColumnInfo(name = "icon")
    val icon: Int?,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0
)