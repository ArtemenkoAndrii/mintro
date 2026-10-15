package com.mas.mobile.domain.analytics

import java.util.Currency

data class OverspendingAlert(
    val expenditureName: String,
    val overspending: Percentage
)

data class TrendEntry(
    val name: String,
    val plan: Double,
    val fact: Double
)

data class Share(
    val name: String,
    val fact: Double,
    val currency: Currency
)

@JvmInline
value class Percentage(val value: Double) {
    operator fun compareTo(other: Percentage): Int {
        return value.compareTo(other.value)
    }

    companion object {
        val P120 = Percentage(120.0)
    }
}

enum class Type {
    AnalyticsTrends, OverspendingAlerts, ExpenditureDistribution
}