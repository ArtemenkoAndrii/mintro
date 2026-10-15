package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mas.mobile.repository.db.entity.Expenditure
import com.mas.mobile.repository.db.entity.ExpenditureData

@Dao
interface ExpenditureDAO {
    @Transaction
    @Query("SELECT * FROM expenditures WHERE budget_id = :budgetId ORDER by display_order, name")
    fun getByBudgetId(budgetId: Int): List<Expenditure>

    @Transaction
    @Query("SELECT * FROM expenditures WHERE budget_id = :budgetId ORDER by display_order, name")
    fun getByBudgetIdLive(budgetId: Int): LiveData<List<Expenditure>>

    @Query("SELECT DISTINCT `name` FROM expenditures")
    fun getUniqueNames(): List<String>

    @Transaction
    @Query("SELECT * FROM expenditures WHERE id = :expenditureId")
    fun getByIdLive(expenditureId: Int): LiveData<Expenditure>

    @Transaction
    @Query("SELECT * FROM expenditures WHERE id = :expenditureId")
    fun getById(expenditureId: Int): Expenditure?

    @Ignore
    suspend fun insert(expenditures: Expenditure): Long {
        return insertExpenditureData(expenditures.data)
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpenditureData(expenditures: ExpenditureData): Long

    @Ignore
    suspend fun update(expenditures: Expenditure) {
        updateExpenditureData(expenditures.data)
    }

    @Update
    suspend fun updateExpenditureData(expenditures: ExpenditureData)

    @Ignore
    suspend fun delete(expenditure: Expenditure) {
        deleteExpenditureData(expenditure.data)
    }

    @Delete
    suspend fun deleteExpenditureData(expenditure: ExpenditureData)

    @Query("DELETE FROM expenditures WHERE id = :expenditureId")
    suspend fun deleteExpenditureById(expenditureId: Int)

    @Upsert
    suspend fun upsert(expenditure: ExpenditureData): Long
}