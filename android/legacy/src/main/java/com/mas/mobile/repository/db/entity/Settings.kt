package com.mas.mobile.repository.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings (
    @PrimaryKey()
    @ColumnInfo(name = "key")
    var key: String = "",

    @ColumnInfo(name = "value")
    var value: String? = null
)

