package com.mas.mobile.domain.budget

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency
import kotlin.test.assertEquals

class BudgetTest {
    private lateinit var testInstance: Budget

    @BeforeEach
    fun setUp() {
        testInstance = getEmptyBudget()
    }

    @Test
    fun `should add expenditure` () {
        testInstance.addExpenditure(FOOD_EXPENDITURE.copy())

        val addedExpenditure = testInstance.budgetDetails.expenditure[0]
        val foundExpenditure = testInstance.getExpenditure(FOOD_EXPENDITURE.id)!!

        // Expenditure checks
        assertEquals(addedExpenditure, foundExpenditure)
        assertEquals(FOOD_EXPENDITURE.name, foundExpenditure.name)
        assertEquals(FOOD_EXPENDITURE.plan, foundExpenditure.plan, TOLERANCE)
        assertEquals(testInstance.id, foundExpenditure.budgetId)
        assertEquals(1, foundExpenditure.displayOrder)

        // Budget checks
        assertEquals(100.00, testInstance.plan, TOLERANCE)
        assertEquals(0.00, testInstance.fact, TOLERANCE)
        assertEquals(0, testInstance.getProgress())
        assertEquals("0.00 / 100.00", testInstance.getStatus())
    }

    @Test
    fun `should update expenditure` () {
        `should add expenditure`()
        val originalExpenditure = testInstance.budgetDetails.expenditure[0]

        testInstance.addExpenditure(originalExpenditure.copy(plan = 300.00))

        // Expenditure checks
        val updatedExpenditure = testInstance.getExpenditure(originalExpenditure.id)!!
        assertEquals(originalExpenditure.id, updatedExpenditure.id)
        assertEquals(originalExpenditure.name, updatedExpenditure.name)
        assertEquals(originalExpenditure.displayOrder, updatedExpenditure.displayOrder)
        assertEquals(300.00, updatedExpenditure.plan, TOLERANCE)

        // Budget checks
        assertEquals(300.00, testInstance.plan, TOLERANCE)
        assertEquals(1, testInstance.budgetDetails.expenditure.size)
    }

    @Test
    fun `should add spending` () {
        `should add expenditure`()

        testInstance.addSpending(FOOD_SPENDING.copy())

        // Spending checks
        val addedSpending = testInstance.budgetDetails.spending[0]
        assertEquals(FOOD_SPENDING.expenditure.name, addedSpending.expenditure.name)
        assertEquals(FOOD_SPENDING.recurrence, addedSpending.recurrence)
        assertEquals(FOOD_SPENDING.amount, addedSpending.amount, TOLERANCE)
        assertEquals(FOOD_SPENDING.date, addedSpending.date)
        assertEquals(FOOD_SPENDING.exchangeInfo, addedSpending.exchangeInfo)

        // Expenditure checks
        val existingExpenditure = testInstance.budgetDetails.expenditure[0]
        assertEquals(1, existingExpenditure.displayOrder)

        // Budget checks
        assertEquals(1, testInstance.budgetDetails.expenditure.size)
        assertEquals(100.00, testInstance.plan)
        assertEquals(20.00, testInstance.fact)
        assertEquals(20, testInstance.getProgress())
        assertEquals("20.00 / 100.00", testInstance.getStatus())
    }

    @Test
    fun `should update spending` () {
        `should add spending`()
        val originalSpending = testInstance.budgetDetails.spending[0]

        testInstance.addSpending(FOOD_SPENDING.copy(amount = 15.00))

        // Spending checks
        val updatedSpending = testInstance.budgetDetails.spending[0]
        assertEquals(originalSpending.id, updatedSpending.id)
        assertEquals(originalSpending.expenditure, updatedSpending.expenditure)
        assertEquals(originalSpending.date, updatedSpending.date)
        assertEquals(originalSpending.recurrence, updatedSpending.recurrence)
        assertEquals(15.00, updatedSpending.amount, TOLERANCE)

        // Budget checks
        assertEquals(1, testInstance.budgetDetails.spending.size)
        assertEquals(15.00, testInstance.fact)
    }

    @Test
    fun `should remove spending` () {
        `should add spending` ()

        testInstance.addSpending(FOOD_SPENDING_2.copy())
        assertEquals(FOOD_SPENDING, testInstance.budgetDetails.spending[0])
        assertEquals(1, testInstance.budgetDetails.expenditure.size)
        assertEquals(100.00, testInstance.plan)
        assertEquals(25.00, testInstance.fact)
        assertEquals(25, testInstance.getProgress())
        assertEquals("25.00 / 100.00", testInstance.getStatus())

        testInstance.removeSpending(FOOD_SPENDING_2.copy())
        assertEquals(FOOD_SPENDING, testInstance.budgetDetails.spending[0])
        assertEquals(1, testInstance.budgetDetails.expenditure.size)
        assertEquals(100.00, testInstance.plan)
        assertEquals(20.00, testInstance.fact)
        assertEquals(20, testInstance.getProgress())
        assertEquals("20.00 / 100.00", testInstance.getStatus())
    }

    @Test
    fun `should add expenditure implicitly` () {
        `should add spending` ()

        testInstance.addSpending(PETROL_SPENDING.copy())

        // Expenditure checks
        assertEquals(FOOD_EXPENDITURE.id, testInstance.budgetDetails.expenditure[0].id)
        assertEquals(FOOD_EXPENDITURE.name, testInstance.budgetDetails.expenditure[0].name)
        assertEquals(1, testInstance.budgetDetails.expenditure[0].displayOrder)
        assertEquals(PETROL_EXPENDITURE.id, testInstance.budgetDetails.expenditure[1].id)
        assertEquals(PETROL_EXPENDITURE.name, testInstance.budgetDetails.expenditure[1].name)
        assertEquals(2, testInstance.budgetDetails.expenditure[1].displayOrder)

        // Budget checks
        assertEquals(2, testInstance.budgetDetails.expenditure.size)
        assertEquals(250.00, testInstance.plan)
        assertEquals(100.00, testInstance.fact)
        assertEquals(40, testInstance.getProgress())
        assertEquals("100.00 / 250.00", testInstance.getStatus())
        assertEquals(1, testInstance.budgetDetails.expenditure[0].displayOrder)
        assertEquals(2, testInstance.budgetDetails.expenditure[1].displayOrder)
    }

    @Test
    fun `should remove expenditure` () {
        `should add spending`()
        val firstExpenditure = testInstance.budgetDetails.expenditure[0]

        testInstance.addExpenditure(PETROL_EXPENDITURE.copy())
        // Expenditure checks
        val secondExpenditure = testInstance.budgetDetails.expenditure[1]
        assertEquals(FOOD_EXPENDITURE.name, firstExpenditure.name)
        assertEquals(1, firstExpenditure.displayOrder)
        assertEquals(PETROL_EXPENDITURE.name, secondExpenditure.name)
        assertEquals(2, secondExpenditure.displayOrder)

        // Budget checks
        assertEquals(2, testInstance.budgetDetails.expenditure.size)
        assertEquals(250.00, testInstance.plan)
        assertEquals(20.00, testInstance.fact)

        testInstance.removeExpenditure(PETROL_EXPENDITURE.copy())
        // Expenditure checks
        assertEquals(FOOD_EXPENDITURE.name, firstExpenditure.name)
        assertEquals(1, firstExpenditure.displayOrder)

        // Budget checks
        assertEquals(1, testInstance.budgetDetails.expenditure.size)
        assertEquals(100.00, testInstance.plan)
        assertEquals(20.00, testInstance.fact)
    }

    @Test
    fun `should find expenditure ignoring case` () {
        `should add spending` ()

        assertEquals(
            FOOD_EXPENDITURE.id,
            testInstance.findExpenditure("food")?.id
        )
        assertNull(
            testInstance.findExpenditure("non-existing")
        )
    }

    companion object {
        const val TOLERANCE = 0.001

        val PETROL_EXPENDITURE = Expenditure(
            ExpenditureId(1),
            "Petrol",
            null,
            150.00,
            0.00,
            "A95",
            BudgetId(0),
            0
        )

        val FOOD_EXPENDITURE = Expenditure(
            ExpenditureId(2),
            "Food",
            null,
            100.00,
            0.00,
            "",
            BudgetId(0),
            0
        )

        val FOOD_SPENDING = Spending(
            SpendingId(1),
            "McDonalds",
            LocalDateTime.now(),
            20.00,
            FOOD_EXPENDITURE,
            null,
            recurrence = Recurrence.Never
        )

        val FOOD_SPENDING_2 = Spending(
            SpendingId(2),
            "Starbucks",
            LocalDateTime.now(),
            5.00,
            FOOD_EXPENDITURE.copy(),
            null,
            recurrence = Recurrence.Never
        )

        val PETROL_SPENDING = Spending(
            SpendingId(3),
            "Shell",
            LocalDateTime.now(),
            80.00,
            PETROL_EXPENDITURE.copy(),
            null,
            recurrence = Recurrence.Never
        )

        private fun getEmptyBudget(): Budget {
            return Budget(
                id = BudgetId(1),
                name = "August 2024",
                plan = 0.00,
                fact = 0.00,
                startsOn = LocalDate.of(2024, 8, 1),
                lastDayAt = LocalDate.of(2024, 8, 31),
                comment = "Comment",
                currency = Currency.getInstance("EUR"),
                lazyLoader = lazy {
                    BudgetDetails(
                        mutableListOf(),
                        mutableListOf()
                    )
                }
            )
        }
    }
}