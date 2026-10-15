package com.mas.mobile.repository.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "expenses")
data class ExpenseReport(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "amount")
    val amount: Double
)
