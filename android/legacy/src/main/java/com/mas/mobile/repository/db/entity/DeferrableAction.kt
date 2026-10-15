package com.mas.mobile.repository.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "deferrable_actions")
data class DeferrableAction(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "type")
    val type: Short,

    @ColumnInfo(name = "increment")
    val increment: Long,

    @ColumnInfo(name = "active_after")
    val activeAfter: LocalDateTime
) {
    companion object {
        const val UNIFORM: Short = 1
        const val PROGRESSIVE: Short = 2
    }
}
