package com.mas.mobile.domain.budget

import com.mas.mobile.presentation.activity.converter.MoneyConverter
import com.mas.mobile.util.CurrencyTools
import java.time.LocalDate
import java.util.Currency

data class Budget(
    val id: BudgetId,
    var name: String = "",
    var plan: Double = 0.0,
    var fact: Double = 0.0,
    var startsOn: LocalDate = LocalDate.now(),
    var lastDayAt: LocalDate = LocalDate.now(),
    var comment: String? = null,
    var currency: Currency = CurrencyTools.getSystemCurrency(),
    private val lazyLoader: Lazy<BudgetDetails>
) {
    val budgetDetails: BudgetDetails
        get() = lazyLoader.value

    fun getProgress() = (fact * 100 / plan).toInt()
    fun getStatus() = "${MoneyConverter.doubleToString(fact)} / ${MoneyConverter.doubleToString(plan)}"
    fun isScheduled() = id.value == SCHEDULED_BUDGET_ID

    fun getExpenditure(expenditureId: ExpenditureId) =
        budgetDetails.expenditure.firstOrNull{ it.id == expenditureId }

    fun findExpenditure(name: String) =
        budgetDetails.expenditure.firstOrNull { it.name.lowercase() == name.trim().lowercase() }

    fun addExpenditure(expenditure: Expenditure) {
        validate(expenditure)
        with(budgetDetails.expenditure) {
            val index = this.indexOfFirst { it.id == expenditure.id }
            if (index == -1) {
                this.add(expenditure)
                expenditure.displayOrder = (budgetDetails.expenditure.maxOfOrNull { it.displayOrder } ?: 0) + 1
            } else {
                this[index] = expenditure
            }
        }
        expenditure.budgetId = this.id
        calculate()
    }

    fun removeExpenditure(expenditure: Expenditure) {
        val item = getExpenditure(expenditure.id)
        if (item != null) {
            budgetDetails.expenditure.remove(item)
            calculate()
        }
    }

    fun addSpending(spending: Spending) {
        if (getExpenditure(spending.expenditure.id) == null) {
            addExpenditure(spending.expenditure)
        }

        val index = budgetDetails.spending.indexOfFirst { it.id == spending.id }
        if (index == -1) {
            budgetDetails.spending.add(spending)
        } else {
            budgetDetails.spending[index] = spending
        }
        calculate()
    }

    fun removeSpending(spending: Spending) {
        budgetDetails.spending.remove(spending)
        calculate()
    }

    private fun calculate() {
        with(budgetDetails) {
            expenditure.forEach { e ->
                e.fact = spending.filter { it.expenditure.id == e.id }.sumOf { it.amount }
            }
        }

        plan = budgetDetails.expenditure.sumOf { it.plan }
        fact = budgetDetails.expenditure.sumOf { it.fact }
    }

    private fun validate(expenditure: Expenditure) {
        val duplicate = budgetDetails.expenditure.any {
            it.name.equals(expenditure.name, ignoreCase = true) && it.id != expenditure.id
        }

        require(!duplicate) { "Expenditure name ${expenditure.name} isn't unique" }
    }

    companion object {
        const val SCHEDULED_BUDGET_ID = -2
    }
}

open class BudgetDetails(
    val expenditure: MutableList<Expenditure>,
    val spending: MutableList<Spending>
)

@JvmInline
value class BudgetId(val value: Int)