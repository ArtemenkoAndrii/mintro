package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.mas.mobile.repository.db.entity.Category

@Dao
interface CategoryDAO {
    @Query("SELECT * FROM categories ORDER BY display_order, name")
    fun getAllLive(): LiveData<List<Category>>

    @Query("SELECT * FROM categories ORDER BY display_order, name")
    fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getById(id: Int): Category?

    @Delete
    suspend fun delete(category: Category)

    @Upsert
    suspend fun upsert(category: Category): Long
}