package com.mas.mobile.domain.budget

import androidx.lifecycle.LiveData

interface ExpenditureRepository {
    fun create(): Expenditure
    fun getExpenditureNames(sortByRating: Boolean, limit: Short): Set<ExpenditureName>
    fun getExpenditure(expenditureId: ExpenditureId): Expenditure?

    fun getLiveExpenditures(budgetId: BudgetId): LiveData<List<Expenditure>>
    fun getExpenditures(budgetId: BudgetId): List<Expenditure>
}
