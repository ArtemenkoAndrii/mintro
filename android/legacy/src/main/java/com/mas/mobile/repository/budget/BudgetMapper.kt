package com.mas.mobile.repository.budget

import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetDetails
import com.mas.mobile.domain.budget.BudgetId
import com.mas.mobile.toCurrency
import com.mas.mobile.repository.db.entity.Budget as BudgetData

object BudgetMapper {
    fun toModel(dto: BudgetData, lazyLoader: Lazy<BudgetDetails>) =
        Budget(
            id = BudgetId(dto.id),
            name = dto.name,
            plan = dto.plan,
            fact = dto.fact,
            startsOn = dto.startsOn,
            lastDayAt = dto.lastDayAt,
            comment = dto.comment,
            currency = dto.currency.toCurrency(),
            lazyLoader = lazyLoader
        )

    fun toDto(model: Budget) =
        BudgetData(
            id = model.id.value,
            name = model.name,
            plan = model.plan,
            fact = model.fact,
            startsOn = model.startsOn,
            lastDayAt = model.lastDayAt,
            comment = model.comment,
            currency = model.currency.currencyCode
        )
}