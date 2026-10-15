package com.mas.mobile.repository.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.mas.mobile.domain.budget.*
import com.mas.mobile.repository.db.config.AppDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Singleton

@Singleton
class SpendingRepositoryImpl(val db: AppDatabase) : SpendingRepository {
    override fun create(): Spending =
        Spending(
            id = SpendingId(db.idGeneratorDAO().generateId().toInt()),
            comment = "",
            date = LocalDateTime.now(),
            amount = 0.0,
            expenditure = Expenditure(
                id = ExpenditureId(-1),
                name = "",
                iconId = null,
                plan = 0.0,
                fact = 0.0,
                comment = "",
                budgetId = BudgetId(-1),
                displayOrder = 0
            ),
            recurrence = Recurrence.Never
        )

    override fun getSpending(id: SpendingId): Spending? =
         db.spendingDao().getById(id.value)?.let {
            it.data.toModel(it.expenditure.toModel())
        }

    override fun getLiveSpendings(from: LocalDate): LiveData<List<Spending>> =
        db.spendingDao().getLiveSpendings(from.atStartOfDay()).map { list ->
            list.map { it.data.toModel(it.expenditure.toModel()) }.toList().filter { it.expenditure.budgetId.value > 0 }
        }

    override fun getLiveSpendings(budgetId: BudgetId): LiveData<List<Spending>> =
        db.spendingDao().getLiveByBudgetId(budgetId.value).map { list ->
            list.map { it.data.toModel(it.expenditure.toModel()) }.toList()
        }

    override fun getLiveSpendings(expenditureId: ExpenditureId): LiveData<List<Spending>> =
        db.spendingDao().getLiveByExpenditureId(expenditureId.value).map { list ->
            list.map { it.data.toModel(it.expenditure.toModel()) }.toList()
        }
}