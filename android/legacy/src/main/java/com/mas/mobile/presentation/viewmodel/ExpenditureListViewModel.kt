package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.map
import com.mas.mobile.R
import com.mas.mobile.domain.Repository
import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetId
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ExpenditureListViewModel @AssistedInject constructor(
    private val coroutineService: CoroutineService,
    private val budgetService: BudgetService,
    @Assisted val budgetId: Int
) : ListViewModel<Expenditure>(coroutineService, RepositoryStub) {
    private lateinit var currentBudget: Budget

    val budget = budgetService.loadLiveBudget(BudgetId(budgetId))
    val expenditures = budget.map { budgetService.expenditureRepository.getExpenditures(it.id) }

    val color = budget.map  { calcColor(it) }

    init {
        budget.observeForever {
            currentBudget = it
        }
    }

    override fun remove(item: Expenditure) {
        coroutineService.backgroundTask {
            currentBudget.removeExpenditure(item)
            budgetService.budgetRepository.save(currentBudget)
        }
    }

    private fun calcColor(budget: Budget) =
        when {
            budget.getProgress() > 150 -> R.color.colorRed
            budget.getProgress() > 100 -> R.color.colorYellow
            else -> R.color.colorAccent
        }

    @AssistedFactory
    interface Factory {
        fun create(budgetId: Int): ExpenditureListViewModel
    }
}

private object RepositoryStub : Repository<Expenditure> {
    override suspend fun save(item: Expenditure) { }
    override suspend fun remove(item: Expenditure) { }
}