package com.mas.mobile.domain.analytics

import com.mas.mobile.domain.budget.BudgetId
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.ExpenditureName
import com.mas.mobile.halfEven
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsService @Inject constructor(
    private val budgetService: BudgetService,
) {
    fun getAnalyticsTrends(expenditureName: ExpenditureName? = null): List<TrendEntry> {
        val budgets = budgetService.budgetRepository.getCompleted()
            .sortedBy { it.startsOn }
            .takeLast(TRENDS_LIMIT)
        return if (expenditureName != null) {
            budgets
                .flatMap { it.budgetDetails.expenditure }
                .filter { it.name.equals(expenditureName.value, ignoreCase = true) }
                .map { e ->
                    TrendEntry(
                        budgets.first { b -> b.id == e.budgetId }.name,
                        e.plan,
                        e.fact
                    )
                }
        } else {
            budgets.map { TrendEntry(it.name, it.plan, it.fact) }
        }
    }

    fun getOverspendingAlerts(threshold: Percentage): List<OverspendingAlert> {
        val budgets = budgetService.budgetRepository.getCompleted()
            .sortedBy { it.startsOn }
            .takeLast(OVERSPENDING_LIMIT)
        return budgets
            .flatMap { it.budgetDetails.expenditure }
            .groupBy { it.name }
            .filter { group -> group.value.size >= OVERSPENDING_LIMIT }
            .map { (name, expenditures) ->
                val totalPlanned = expenditures.sumOf { it.plan }
                val totalFact = expenditures.sumOf { it.fact }
                val percentage = if (totalPlanned > 0) {
                    val value = (totalFact * 100) / totalPlanned
                    Percentage(value.halfEven())
                } else {
                    threshold
                }
                OverspendingAlert(name, percentage)
            }
            .filter { it.overspending >= threshold }
            .sortedBy { it.overspending.value }
    }

    fun getExpenditureDistribution(budgetId: BudgetId): List<Share> {
        val budget = budgetService.budgetRepository.getBudget(budgetId.value)
        return budget?.budgetDetails?.expenditure?.sortedByDescending { it.fact }
            ?.map { Share(it.name, it.fact, budget.currency) }
            ?: emptyList()
    }

    companion object {
        const val TRENDS_LIMIT = 12
        const val OVERSPENDING_LIMIT = 3
    }
}
