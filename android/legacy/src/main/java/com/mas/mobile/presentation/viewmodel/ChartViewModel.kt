package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mas.mobile.domain.analytics.AnalyticsService
import com.mas.mobile.domain.analytics.ChartShown
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.OverspendingAlert
import com.mas.mobile.domain.analytics.Percentage
import com.mas.mobile.domain.analytics.Share
import com.mas.mobile.domain.analytics.TrendEntry
import com.mas.mobile.domain.analytics.Type
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.ExpenditureName
import com.mas.mobile.domain.budget.ExpenditureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ChartViewModel @AssistedInject constructor(
    private val eventLogger: EventLogger,
    private val analyticsService: AnalyticsService,
    private val budgetService: BudgetService,
    expenditureRepository: ExpenditureRepository,
    @Assisted private val type: Type,
) : ViewModel() {
    val analyticsTrends = MutableLiveData<List<TrendEntry>>()
    val overspendingAlerts = MutableLiveData<List<OverspendingAlert>>()
    val expenditureDistribution = MutableLiveData<List<Share>>()

    val filter = MutableLiveData<String?>()
    val availableFilterValues: List<String> =
        when(type) {
            Type.AnalyticsTrends ->
                expenditureRepository.getExpenditureNames(true, 100).map { it.value }
            Type.ExpenditureDistribution ->
                budgetService.budgetRepository.getCompleted().map { it.name }
            else -> emptyList()
        }

    init {
        run(null)
        filter.observeForever {
            run(it)
        }
    }

    private fun run(filter: String?) {
        when(type) {
            Type.AnalyticsTrends -> analyticsTrends.value = getAnalyticsTrends(filter)
            Type.OverspendingAlerts -> overspendingAlerts.value = getOverspendingAlerts()
            Type.ExpenditureDistribution -> expenditureDistribution.value = getExpenditureDistribution(filter)
        }
    }

    private fun getAnalyticsTrends(expenditureName: String?): List<TrendEntry> {
        eventLogger.log(ChartShown(ChartShown.Type.Trends))
        return analyticsService.getAnalyticsTrends(expenditureName?.let { ExpenditureName(it) } )
    }

    private fun getOverspendingAlerts(): List<OverspendingAlert> {
        eventLogger.log(ChartShown(ChartShown.Type.Overspending))
        return analyticsService.getOverspendingAlerts(Percentage.P120)
    }

    private fun getExpenditureDistribution(budgetName: String? = null): List<Share> {
        eventLogger.log(ChartShown(ChartShown.Type.Distribution))

        val name = budgetName ?: budgetService.getActiveBudget().name
        val result = budgetService.budgetRepository.getBudgetByName(name)?.let {
            analyticsService.getExpenditureDistribution(it.id)
        }
        return result ?: emptyList()
    }

    @AssistedFactory
    interface Factory {
        fun create(action: Type): ChartViewModel
    }
}