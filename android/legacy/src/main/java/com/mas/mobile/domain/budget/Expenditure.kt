package com.mas.mobile.domain.budget

import kotlin.math.absoluteValue

data class Expenditure (
    val id: ExpenditureId,
    var name: String,
    var iconId: IconId?,
    var plan: Double,
    var fact: Double,
    var comment: String,
    var budgetId: BudgetId,
    var displayOrder: Int
) {
    val progress
        get() = if (plan.absoluteValue < 0.001) {
            100
        } else {
            (fact * 100 / plan).toInt()
        }
}

@JvmInline
value class ExpenditureId(val value: Int)

@JvmInline
value class ExpenditureName(val value: String)