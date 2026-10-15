package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.mas.mobile.repository.db.entity.Qualifier

@Dao
interface QualifierDAO {
    @Query("SELECT * FROM qualifiers WHERE type=:type ORDER BY name")
    fun getQualifiersLive(type: Short): LiveData<List<Qualifier>>

    @Query("SELECT * FROM qualifiers WHERE type=:type ORDER BY name")
    fun getQualifiers(type: Short): List<Qualifier>

    @Insert
    suspend fun insert(qualifier: Qualifier): Long

    @Delete
    suspend fun delete(qualifier: Qualifier)

    @Upsert
    suspend fun upsert(qualifier: Qualifier): Long
}