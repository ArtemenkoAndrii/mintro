package com.mas.mobile.presentation.viewmodel

import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.service.CoroutineService
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class BudgetListViewModel @AssistedInject constructor(
    budgetRepository: BudgetRepository,
    coroutineService: CoroutineService,
    private val budgetService: BudgetService,
): ListViewModel<Budget>(coroutineService, budgetRepository) {
    val budgets = budgetRepository.live.getBudgets()

    fun createBudget() = budgetService.createNext()

    fun isChangeable(item: Budget) =
        budgetService.isPeriodChangeable(item)

    @AssistedFactory
    interface Factory {
        fun create(): BudgetListViewModel
    }
}
