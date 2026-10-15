package com.mas.mobile.repository.db.dao

import androidx.room.*
import com.mas.mobile.repository.db.entity.Settings

@Dao
interface SettingsDAO {
    @Query("SELECT * FROM settings")
    fun getAll(): List<Settings>

    @Upsert
    suspend fun upsert(settings: Settings): Long
}