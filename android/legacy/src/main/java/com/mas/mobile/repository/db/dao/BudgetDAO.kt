package com.mas.mobile.repository.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mas.mobile.repository.db.entity.Budget
import java.time.LocalDate

@Dao
interface BudgetDAO {
    @Query("SELECT * FROM budgets WHERE id > 0 ORDER BY startsOn DESC")
    fun getAllLive(): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id > 0 ORDER BY startsOn DESC")
    fun getAll(): List<Budget>

    @Query("SELECT * FROM budgets WHERE id > 0 AND lastDayAt < :date ORDER BY startsOn DESC")
    fun getCompleted(date: LocalDate = LocalDate.now()): List<Budget>

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun getByIdLive(budgetId: Int): LiveData<Budget>

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun getById(budgetId: Int): Budget?

    @Query("SELECT * FROM budgets WHERE name = :name")
    fun getByName(name: String): Budget?

    @Query("SELECT b.* FROM budgets b JOIN expenditures e ON b.id = e.budget_id JOIN spendings s ON s.expenditure_id = e.id WHERE s.id = :id")
    fun getBySpendingId(id: Int): Budget?

    @Query("SELECT * FROM budgets WHERE id > 0 AND lastDayAt < :date ORDER BY lastDayAt DESC LIMIT 1")
    fun getLatestEndedOn(date: LocalDate): Budget?

    @Query("SELECT * FROM budgets WHERE id > 0 AND :date BETWEEN startsOn AND lastDayAt")
    fun getActiveOn(date: LocalDate): Budget?

    @Upsert
    suspend fun upsert(budgets: Budget): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budgets: Budget): Long

    @Update
    suspend fun update(budgets: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}