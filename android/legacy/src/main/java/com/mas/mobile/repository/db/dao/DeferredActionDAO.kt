package com.mas.mobile.repository.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mas.mobile.repository.db.entity.DeferrableAction
import java.time.LocalDateTime

@Dao
interface DeferredActionDAO {
    @Query("SELECT * FROM deferrable_actions WHERE `key` = :value")
    fun getByKey(value: String): DeferrableAction?

    @Query("DELETE FROM deferrable_actions WHERE active_after < :olderThen")
    suspend fun purge(olderThen: LocalDateTime)

    @Upsert
    suspend fun upsert(item: DeferrableAction)

    @Delete
    suspend fun remove(item: DeferrableAction)
}