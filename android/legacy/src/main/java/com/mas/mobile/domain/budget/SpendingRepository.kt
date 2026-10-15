package com.mas.mobile.domain.budget

import androidx.lifecycle.LiveData
import java.time.LocalDate

interface SpendingRepository {
    fun create(): Spending
    fun getSpending(id: SpendingId): Spending?

    fun getLiveSpendings(from: LocalDate): LiveData<List<Spending>>
    fun getLiveSpendings(budgetId: BudgetId): LiveData<List<Spending>>
    fun getLiveSpendings(expenditureId: ExpenditureId): LiveData<List<Spending>>
}