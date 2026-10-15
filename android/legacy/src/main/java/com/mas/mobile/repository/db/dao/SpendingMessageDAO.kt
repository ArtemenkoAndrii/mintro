package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mas.mobile.repository.db.entity.SpendingMessage
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface SpendingMessageDAO {
    @Query("SELECT * FROM spending_messages WHERE receivedAt >= :from ORDER BY receivedAt DESC")
    fun getLiveMessages(from: LocalDateTime): LiveData<List<SpendingMessage>>

    @Query("SELECT * FROM spending_messages WHERE id = :id")
    fun getById(id: Int): SpendingMessage?

    @Query("SELECT * FROM spending_messages WHERE spending_id = :id")
    fun getBySpendingId(id: Int): SpendingMessage?

    @Query("SELECT * FROM spending_messages WHERE name=:name")
    fun getBySender(name: String): List<SpendingMessage>

    @Query("SELECT count(1) FROM spending_messages WHERE is_new == 1")
    fun countUnreadLive(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(spendingMessage: SpendingMessage): Long

    @Update
    suspend fun update(spendingMessage: SpendingMessage)

    @Delete
    suspend fun delete(spendingMessage: SpendingMessage)

    @Upsert
    suspend fun upsert(spendingMessage: SpendingMessage): Long

    @Query("UPDATE spending_messages SET spending_id = NULL  WHERE spending_id = :spendingId")
    suspend fun removeBindSpendingById(spendingId: Int)
}