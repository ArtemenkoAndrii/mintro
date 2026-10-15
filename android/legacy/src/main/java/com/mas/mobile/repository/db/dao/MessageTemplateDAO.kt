package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mas.mobile.repository.db.entity.MessageTemplate

@Dao
interface MessageTemplateDAO {
    @Query("SELECT * FROM message_template")
    fun getAllLive(): LiveData<List<MessageTemplate>>

    @Query("SELECT * FROM message_template")
    fun getAll(): List<MessageTemplate>

    @Query("SELECT * FROM message_template WHERE id = :id")
    fun getById(id: Int): MessageTemplate?

    @Delete
    suspend fun delete(messageTemplate: MessageTemplate)

    @Upsert
    suspend fun upsert(messageTemplate: MessageTemplate): Long
}