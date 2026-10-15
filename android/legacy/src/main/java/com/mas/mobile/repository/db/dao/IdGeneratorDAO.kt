package com.mas.mobile.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.mas.mobile.repository.db.entity.IdGenerator

@Dao
interface IdGeneratorDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun generateId(id: IdGenerator = IdGenerator()): Long
}