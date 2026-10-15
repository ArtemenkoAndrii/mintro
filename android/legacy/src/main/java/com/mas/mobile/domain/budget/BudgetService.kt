package com.mas.mobile.domain.budget

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.ScheduledBudgetNotFound
import com.mas.mobile.domain.settings.Period
import com.mas.mobile.domain.settings.SettingsRepository
import com.mas.mobile.service.ErrorHandler
import com.mas.mobile.service.ResourceService
import com.mas.mobile.service.TaskService
import com.mas.mobile.util.DateTool
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetService @Inject constructor(
    private val resourceService: ResourceService,
    private val settingsRepository: SettingsRepository,
    private val spendingRepository: SpendingRepository,
    private val exchangeRepository: ExchangeRepository,
    private val categoryRepository: CategoryRepository,
    private val errorHandler: ErrorHandler,
    private val coroutineService: TaskService,
    private val eventLogger: EventLogger,
    val budgetRepository: BudgetRepository,
    val expenditureRepository: ExpenditureRepository,
) {
    private val settings
        get() = settingsRepository.get()

    private var activeSource: LiveData<Budget> = MutableLiveData()
    private val activeLive = MediatorLiveData<Budget>()

    fun reloadActiveBudget() {
        activeLive.removeSource(activeSource)
        activeSource = budgetRepository.live.getBudget(getActiveBudget().id)

        activeLive.addSource(activeSource) {
            if (it == null) {
                reloadActiveBudget()
            } else {
                activeLive.postValue(it)
            }
        }
    }

    fun loadBudgetOrGetActive(budgetId: BudgetId): Budget =
        if (budgetId.value == -1) {
            getActiveBudget()
        } else {
            budgetRepository.getBudget(budgetId.value)
        }
        ?: throw BudgetException("Budget ${budgetId.value} not found.")

    fun loadLiveBudget(budgetId: BudgetId): LiveData<Budget> =
        if (budgetId.value == -1) {
            this.activeLive
        } else {
            budgetRepository.live.getBudget(budgetId)
        }

    fun getActiveBudget() =
        budgetRepository.getOnDate(LocalDate.now()) ?: createNext()

    fun isPeriodChangeable(budget: Budget) =
        budgetRepository.getLast()?.id == budget.id

    fun createNext() = runBlocking {
        val startDate = budgetRepository.getLast()?.lastDayAt?.plusDays(1)?.let {
            maxOf(it, LocalDate.now())
        } ?: LocalDate.now()

        val budget = createBudget(startDate).also {
            it.currency = settings.currency
            uniquifyName(it)
            populateExpenditures(it)
        }

        budgetRepository.save(budget)

        return@runBlocking budget
    }

    suspend fun spend(date: LocalDateTime,
                      amount: Double,
                      comment: String,
                      expenditureName: String,
                      currency: Currency? = null): SpendingId {
        val budget = getActiveBudget()
        val expenditure = budget.findExpenditure(expenditureName)
            ?: createExpenditure(expenditureName).also { it.budgetId = budget.id }

        val spending = spendingRepository.create().also {
            it.comment = comment.trim()
            it.date = date
            it.amount = amount
            it.expenditure = expenditure
        }

        if (currency != null && currency != budget.currency) {
            spending.exchangeInfo = ExchangeInfo(
                rawAmount = amount,
                rate = getRate(currency),
                currency = currency
            )
            spending.exchange()
        }

        budget.addSpending(spending)
        budgetRepository.save(budget)

        return spending.id
    }

    fun getRate(currency: Currency): Double = runBlocking {
        exchangeRepository.getRate(getActiveBudget().currency, currency).getOrElse {
            Log.e(this::class.java.name, it.message, it)
            errorHandler.toast("Unable to retrieve the current exchange rate. Please try again later.")
            0.00
        }
    }

    fun createScheduledSpendings() =
        coroutineService.backgroundTask {
            val scheduledBudget = budgetRepository.getBudget(Budget.SCHEDULED_BUDGET_ID)
            if (scheduledBudget != null) {
                scheduledBudget.budgetDetails.spending
                    .filter { it.scheduledDate != null && it.scheduledDate!! < LocalDateTime.now() }
                    .forEach { scheduled ->
                        val date = scheduled.scheduledDate!!
                        spend(
                            date = date,
                            amount = scheduled.amount,
                            comment = scheduled.comment,
                            expenditureName = scheduled.expenditure.name,
                            currency = scheduled.exchangeInfo?.currency,
                        )
                        scheduled.date = date
                    }
                budgetRepository.save(scheduledBudget)
            } else {
                eventLogger.log(ScheduledBudgetNotFound)
            }
        }

    fun createExpenditure(expenditureName: String): Expenditure {
        val name = expenditureName.trim()
        val category = categoryRepository.getAll().firstOrNull { it.name.equals(name, true) }

        return expenditureRepository.create().also { expenditure ->
            expenditure.name = expenditureName
            expenditure.iconId = category?.iconId
            expenditure.plan = category?.plan ?: 0.00
        }
    }

    init {
        reloadActiveBudget()
    }

    private fun populateExpenditures(budget: Budget) {
        val categories = categoryRepository.getAll().filter { it.isActive }
        categories.forEach { category ->
            val expenditure = expenditureRepository.create().also {
                it.name = category.name
                it.plan = category.plan
                it.iconId = category.iconId
                it.displayOrder = category.displayOrder
            }
            budget.addExpenditure(expenditure)
        }
    }

    private fun uniquifyName(budget: Budget) {
        var name = budget.name
        var i = DEFAULT_NAME_SUFFIX
        while (budgetRepository.getBudgetByName(name) != null) {
            name = budget.name + "($i)"
            i++
        }

        budget.name = name
    }

    @VisibleForTesting
    internal fun createBudget(startDate: LocalDate): Budget {
        val budget = budgetRepository.createBudget()
        when (settings.period) {
            Period.MONTH -> prepareMonthBudget(budget, startDate)
            Period.WEEK -> prepareWeekBudget(budget, startDate)
            Period.TWO_WEEKS -> prepareWeekBudget(budget, startDate).also {
                budget.lastDayAt = budget.lastDayAt.plusDays(7)
            }
            Period.QUARTER -> prepareQuarterBudget(budget, startDate)
            Period.YEAR -> prepareYearBudget(budget, startDate)
        }
        return budget
    }

    private fun prepareWeekBudget(budget: Budget, actualStartDate: LocalDate) {
        val periodStartDay = settings.startDayOfWeek
        val diff = if (actualStartDate.dayOfWeek.value >= periodStartDay.value) {
            actualStartDate.dayOfWeek.value - periodStartDay.value
        } else {
            7 - periodStartDay.value + actualStartDate.dayOfWeek.value
        }
        val estimatedStartDate = actualStartDate.minusDays(diff.toLong())
        val estimatedEndDate = estimatedStartDate.plusDays(6)

        budget.name = DateTool.formatToWeekWithDate(estimatedStartDate)
        budget.startsOn = actualStartDate
        budget.lastDayAt = estimatedEndDate
        budget.comment = generateComment(estimatedStartDate, actualStartDate)
    }

    private fun prepareMonthBudget(budget: Budget, actualStartDate: LocalDate) {
        val periodStartDay = settings.startDayOfMonth.value
        val estimatedStartDate = if (actualStartDate.dayOfMonth >= periodStartDay) {
            actualStartDate.withDayOfMonth(periodStartDay)
        } else {
            actualStartDate.minusMonths(1).withDayOfMonth(periodStartDay)
        }
        val estimatedEndDate = estimatedStartDate.plusMonths(1).minusDays(1)

        budget.name = DateTool.formatToMonthWithYear(estimatedStartDate)
        budget.startsOn = actualStartDate
        budget.lastDayAt = estimatedEndDate
        budget.comment = generateComment(estimatedStartDate, actualStartDate)
    }

    private fun prepareQuarterBudget(budget: Budget, actualStartDate: LocalDate) {
        val quarterlyMonth = listOf(10, 7, 4, 1).first{ actualStartDate.month.value >= it }
        val estimatedStartDate = actualStartDate.withMonth(quarterlyMonth).withDayOfMonth(1)
        val estimatedEndDate = estimatedStartDate.plusMonths(2).let { it.withDayOfMonth(it.month.length(it.isLeapYear)) }

        budget.name = DateTool.formatToQuarterWithYear(estimatedStartDate)
        budget.startsOn = actualStartDate
        budget.lastDayAt = estimatedEndDate
        budget.comment = generateComment(estimatedStartDate, actualStartDate)
    }

    private fun prepareYearBudget(budget: Budget, actualStartDate: LocalDate) {
        val estimatedStartDate = actualStartDate.withMonth(1).withDayOfMonth(1)
        val estimatedEndDate = estimatedStartDate.withMonth(12).let { it.withDayOfMonth(it.month.length(it.isLeapYear)) }

        budget.name = DateTool.formatToYear(estimatedStartDate)
        budget.startsOn = actualStartDate
        budget.lastDayAt = estimatedEndDate
        budget.comment = generateComment(estimatedStartDate, actualStartDate)
    }

    private fun generateComment(estimatedStartDate: LocalDate, actualStartDate: LocalDate) =
        if (estimatedStartDate < actualStartDate) {
            resourceService.budgetTruncatedComment(estimatedStartDate)
        } else {
            ""
        }

    companion object {
        const val DEFAULT_NAME_SUFFIX = 2
    }
}