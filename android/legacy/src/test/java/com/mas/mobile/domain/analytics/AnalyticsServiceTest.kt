package com.mas.mobile.domain.analytics

import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetDetails
import com.mas.mobile.domain.budget.BudgetId
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.domain.budget.ExpenditureId
import com.mas.mobile.domain.budget.ExpenditureName
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class AnalyticsServiceTest {
    private val mockBudgetService = mockk<BudgetService>(relaxed = true)
    private val mockBudgetRepository = mockk<BudgetRepository>(relaxed = true)

    private val testInstance = AnalyticsService(mockBudgetService)

    @BeforeEach
    fun setUp() {
        every { mockBudgetService.budgetRepository } returns mockBudgetRepository
        every { mockBudgetRepository.getCompleted() } returns BUDGETS
        every { mockBudgetRepository.getBudget(any()) } returns BUDGETS[0]
    }

    @Test
    fun `should build trends by budgets`() {
        val result = testInstance.getAnalyticsTrends()
        with(result[0]) {
            assertEquals("Budget1", name)
            assertEquals(63.00, plan, 0.001)
            assertEquals(125.50, fact, 0.001)
        }
        with(result[1]) {
            assertEquals("Budget2", name)
            assertEquals(55.00, plan, 0.001)
            assertEquals(81.08, fact, 0.001)
        }
        with(result[2]) {
            assertEquals("Budget3", name)
            assertEquals(55.00, plan, 0.001)
            assertEquals(62.00, fact, 0.001)
        }
    }

    @Test
    fun `should build trends by expenditures`() {
        val result = testInstance.getAnalyticsTrends(ExpenditureName("TestExpenditure1"))
        with(result[0]) {
            assertEquals("Budget1", name)
            assertEquals(10.00, plan, 0.001)
            assertEquals(9.50, fact, 0.001)
        }
        with(result[1]) {
            assertEquals("Budget2", name)
            assertEquals(10.00, plan, 0.001)
            assertEquals(8.97, fact, 0.001)
        }
        with(result[2]) {
            assertEquals("Budget3", name)
            assertEquals(10.00, plan, 0.001)
            assertEquals(9.00, fact, 0.001)
        }
    }

    @Test
    fun `should build alerts`() {
        val result = testInstance.getOverspendingAlerts(Percentage.P120)
        with(result[0]) {
            assertEquals("TestExpenditure5", this.expenditureName)
            assertEquals(120.00, this.overspending.value, 0.001)
        }
        with(result[1]) {
            assertEquals("TestExpenditure3", this.expenditureName)
            assertEquals(125.56, this.overspending.value, 0.001)
        }
    }

    @Test
    fun `should build alerts distribution`() {
        val result = testInstance.getExpenditureDistribution(BudgetId(1))
        assertEquals(5, result.size)
    }

    private companion object {
        val EXP2 = mutableListOf(
            Expenditure(ExpenditureId(6), "TestExpenditure1", null, 10.00, 8.97, "No comments", BudgetId(2), 0),
            Expenditure(ExpenditureId(7), "TestExpenditure2", null, 15.00, 16.11, "No comments", BudgetId(2), 1),
            Expenditure(ExpenditureId(8), "TestExpenditure3", null, 30.00, 45.00, "No comments", BudgetId(2),2),
            Expenditure(ExpenditureId(9), "TestExpenditure5", null, 0.00, 11.00, "No comments", BudgetId(2),3),
        )
        val EXP1 = mutableListOf(
            Expenditure(ExpenditureId(1), "TestExpenditure1", null, 10.00, 9.50, "No comments", BudgetId(1), 0),
            Expenditure(ExpenditureId(2), "TestExpenditure2", null, 15.00, 16.00, "No comments", BudgetId(1), 1),
            Expenditure(ExpenditureId(3), "TestExpenditure3", null, 30.00, 35.00, "No comments", BudgetId(1), 2),
            Expenditure(ExpenditureId(4), "TestExpenditure4", null, 8.00, 40.00, "No comments", BudgetId(1), 3),
            Expenditure(ExpenditureId(5), "TestExpenditure5", null, 0.00, 25.00, "No comments", BudgetId(1), 4),
        )
        val EXP3 = mutableListOf(
            Expenditure(ExpenditureId(6), "TestExpenditure1", null, 10.00, 9.00, "No comments", BudgetId(3), 0),
            Expenditure(ExpenditureId(7), "TestExpenditure2", null, 15.00, 15.00, "No comments", BudgetId(3), 1),
            Expenditure(ExpenditureId(8), "TestExpenditure3", null, 30.00, 33.00, "No comments", BudgetId(3), 2),
            Expenditure(ExpenditureId(9), "TestExpenditure5", null, 0.00, 5.00, "No comments", BudgetId(3), 3),
        )
        val BUDGETS = listOf(
            Budget(
                id = BudgetId(1),
                name = "Budget1",
                plan = EXP1.sumOf { it.plan },
                fact = EXP1.sumOf { it.fact },
                startsOn = LocalDate.now().minusMonths(1),
                lazyLoader = lazy { BudgetDetails(EXP1, mutableListOf()) }
            ),
            Budget(
                id = BudgetId(2),
                name = "Budget2",
                plan = EXP2.sumOf { it.plan },
                fact = EXP2.sumOf { it.fact },
                startsOn = LocalDate.now().minusMonths(1),
                lazyLoader = lazy { BudgetDetails(EXP2, mutableListOf()) }
            ),
            Budget(
                id = BudgetId(3),
                name = "Budget3",
                plan = EXP3.sumOf { it.plan },
                fact = EXP3.sumOf { it.fact },
                startsOn = LocalDate.now().minusMonths(1),
                lazyLoader = lazy { BudgetDetails(EXP3, mutableListOf()) }
            )
        )
    }
}