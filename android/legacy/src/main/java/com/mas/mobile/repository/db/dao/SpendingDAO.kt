package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mas.mobile.repository.db.entity.Spending
import com.mas.mobile.repository.db.entity.SpendingData
import java.time.LocalDateTime

@Dao
interface SpendingDAO {
    @Transaction
    @Query("SELECT * FROM spendings WHERE date >= :from ORDER BY date DESC")
    fun getLiveSpendings(from: LocalDateTime): LiveData<List<Spending>>

    @Transaction
    @Query("SELECT * FROM spendings WHERE id = :spendingId")
    fun getById(spendingId: Int): Spending?

    @Transaction
    @Query("SELECT * FROM spendings WHERE id = :spendingId")
    fun getLiveById(spendingId: Int): LiveData<Spending>

    @Transaction
    @Query("SELECT * FROM spendings WHERE expenditure_id = :expenditureId ORDER BY date DESC")
    fun getByExpenditureId(expenditureId: Int): List<Spending>

    @Transaction
    @Query("SELECT s.* FROM spendings s INNER JOIN expenditures e ON s.expenditure_id = e.id WHERE e.budget_id = :budgetId ORDER BY s.date DESC")
    fun getByBudgetId(budgetId: Int): List<Spending>

    @Transaction
    @Query("SELECT s.* FROM spendings s INNER JOIN expenditures e ON s.expenditure_id = e.id WHERE e.budget_id = :budgetId ORDER BY s.date DESC")
    fun getLiveByBudgetId(budgetId: Int): LiveData<List<Spending>>

    @Transaction
    @Query("SELECT * FROM spendings WHERE expenditure_id = :expenditureId ORDER BY date DESC")
    fun getLiveByExpenditureId(expenditureId: Int): LiveData<List<Spending>>

    @Ignore
    suspend fun insert(spending: Spending) =
        insertSpendingData(spending.data)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpendingData(spendings: SpendingData): Long

    @Ignore
    suspend fun update(spending: Spending) {
        updateSpendingData(spending.data)
    }

    @Update
    suspend fun updateSpendingData(spendings: SpendingData)

    @Ignore
    suspend fun delete(spending: Spending) {
        deleteSpendingData(spending.data)
    }

    @Delete
    suspend fun deleteSpendingData(spending: SpendingData)

    @Query("DELETE FROM spendings WHERE id = :spendingId")
    suspend fun deleteSpendingById(spendingId: Int)

    @Upsert
    suspend fun upsert(spending: SpendingData): Long
}