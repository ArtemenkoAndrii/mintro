package com.mas.mobile.testing

import com.mas.mobile.repository.db.config.AppDatabase
import com.mas.mobile.repository.db.entity.Budget
import com.mas.mobile.repository.db.entity.Category
import com.mas.mobile.repository.db.entity.ExpenditureData
import com.mas.mobile.repository.db.entity.MessageTemplate
import com.mas.mobile.repository.db.entity.Qualifier
import com.mas.mobile.repository.db.entity.SpendingData
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Minimal DAO-level fixture helpers for instrumented tests, seeding [TestPersistenceModule]'s
 * empty in-memory database directly - no dependency on the shipped seed data in `DML.kt`.
 *
 * Seed fixtures *before* launching an `ActivityScenario`/navigating to a screen that observes
 * them: Room's `LiveData` query results only refresh asynchronously on data change, which
 * Espresso's default idling does not track, so seeding after a screen is already observing a
 * query risks a flaky "not visible yet" failure. Seeding before the first observation means the
 * initial query already returns the fixture, with nothing to wait for.
 */
object TestFixtures {
    /**
     * Inserts a budget whose date range covers [LocalDate.now], so `BudgetService.getActiveBudget()`
     * (called when the app's first fragment attaches) finds it via `getOnDate(today)` instead of
     * auto-creating a separate budget - tests get one known, fully-controlled active budget.
     */
    fun seedActiveBudget(
        db: AppDatabase,
        name: String = "Test Budget",
        currency: String = "USD"
    ): Int = runBlocking {
        db.budgetDao().insert(
            Budget(
                name = name,
                startsOn = LocalDate.now().minusDays(1),
                lastDayAt = LocalDate.now().plusDays(29),
                currency = currency
            )
        ).toInt()
    }

    /** Inserts a historical (non-active) budget with the given date range. */
    fun seedBudget(
        db: AppDatabase,
        name: String,
        startsOn: LocalDate,
        lastDayAt: LocalDate,
        currency: String = "USD"
    ): Int = runBlocking {
        db.budgetDao().insert(
            Budget(name = name, startsOn = startsOn, lastDayAt = lastDayAt, currency = currency)
        ).toInt()
    }

    /**
     * Also bumps the parent budget's own `plan`/`fact` columns by this expenditure's amounts,
     * mirroring what `Budget.addExpenditure()` -> `calculate()` does in production - those are
     * stored columns on the `budgets` row, not derived from `expenditures` at read time, so a
     * bare DAO insert here would otherwise leave the budget's own plan/fact at 0.
     */
    fun seedExpenditure(
        db: AppDatabase,
        budgetId: Int,
        name: String,
        plan: Double = 0.0,
        fact: Double = 0.0
    ): Int = runBlocking {
        val id = db.expenditureDao().insertExpenditureData(
            ExpenditureData(name = name, plan = plan, fact = fact, budget_id = budgetId, icon = null)
        ).toInt()

        db.budgetDao().getById(budgetId)?.let { budget ->
            budget.plan += plan
            budget.fact += fact
            db.budgetDao().update(budget)
        }

        id
    }

    /**
     * Also bumps the parent expenditure's `fact` column, mirroring `Budget.calculate()` in
     * production - a bare DAO insert here would otherwise leave the expenditure's `fact` at 0.
     */
    fun seedSpending(
        db: AppDatabase,
        expenditureId: Int,
        amount: Double,
        comment: String = "",
        date: LocalDateTime = LocalDateTime.now()
    ): Int = runBlocking {
        val id = db.spendingDao().insertSpendingData(
            SpendingData(comment = comment, date = date, amount = amount, expenditureId = expenditureId, recurrence = "Never")
        ).toInt()

        db.expenditureDao().getById(expenditureId)?.let { expenditure ->
            expenditure.data.fact += amount
            db.expenditureDao().updateExpenditureData(expenditure.data)
        }

        id
    }

    fun seedCategory(
        db: AppDatabase,
        id: Int,
        name: String,
        merchants: List<String> = emptyList(),
        plan: Double = 0.0,
        active: Boolean = true
    ) = runBlocking {
        db.categoryDAO().upsert(
            Category(id = id, name = name, plan = plan, isActive = active, description = "", merchants = merchants, icon = null)
        )
    }

    fun seedMessageTemplate(
        db: AppDatabase,
        sender: String,
        pattern: String,
        example: String,
        currency: String = "USD",
        enabled: Boolean = true
    ) = runBlocking {
        db.messageTemplateDAO().upsert(
            MessageTemplate(sender = sender, pattern = pattern, example = example, currency = currency, isEnabled = enabled)
        )
    }

    /**
     * Inserts a `Qualifier` keyword row. The shipped [Qualifier.CATCH]/[Qualifier.SKIP]/
     * [Qualifier.BLACKLIST] seed data (`DML.kt`) is never loaded into the empty in-memory test DB,
     * so `QualifierService.isRecommended` needs at least one `CATCH` row seeded here to ever
     * classify a message as `Message.Recommended`.
     */
    fun seedQualifier(
        db: AppDatabase,
        type: Short,
        name: String
    ): Int = runBlocking {
        db.qualifierDAO().insert(Qualifier(name = name, type = type)).toInt()
    }
}
