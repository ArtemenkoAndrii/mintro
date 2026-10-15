package com.mas.mobile.domain.backup

import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetDetails
import com.mas.mobile.domain.budget.BudgetId
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.ExchangeInfo
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.domain.budget.ExpenditureId
import com.mas.mobile.domain.budget.ExpenditureRepository
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.domain.budget.Recurrence
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.domain.budget.SpendingId
import com.mas.mobile.domain.budget.SpendingRepository
import com.mas.mobile.domain.message.Message
import com.mas.mobile.domain.message.MessageId
import com.mas.mobile.domain.message.MessageRepository
import com.mas.mobile.domain.message.MessageTemplateId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupServiceTest {
    private val mockBudgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val mockExpenditureRepository = mockk<ExpenditureRepository>(relaxed = true)
    private val mockSpendingRepository = mockk<SpendingRepository>(relaxed = true)
    private val mockMessageRepository = mockk<MessageRepository>(relaxed = true)

    private val testInstance = BackupService(
        mockBudgetRepository,
        mockExpenditureRepository,
        mockSpendingRepository,
        mockMessageRepository
    )

    private val budgetSlot = slot<Budget>()
    private val messageSlots = mutableListOf<Message>()

    private var nextId = 100

    @BeforeEach
    fun setUp() {
        coEvery { mockBudgetRepository.save(capture(budgetSlot)) } returns Unit
        coEvery { mockMessageRepository.save(capture(messageSlots)) } returns Unit
    }

    @Test
    fun `buildExport maps budgets, expenditures, spendings and their messages`() {
        val expenditure = Expenditure(
            id = ExpenditureId(1),
            name = "Food",
            iconId = IconId(5),
            plan = 100.0,
            fact = 25.0,
            comment = "comment",
            budgetId = BudgetId(1),
            displayOrder = 0
        )
        val spending = Spending(
            id = SpendingId(10),
            comment = "Coffee",
            date = LocalDateTime.of(2024, 1, 1, 10, 0),
            amount = 5.0,
            expenditure = expenditure,
            exchangeInfo = ExchangeInfo(rawAmount = 5.0, rate = 1.1, currency = Currency.getInstance("USD")),
            recurrence = Recurrence.Never
        )
        val budget = Budget(
            id = BudgetId(1),
            name = "September 2024",
            plan = 100.0,
            fact = 25.0,
            startsOn = LocalDate.of(2024, 9, 1),
            lastDayAt = LocalDate.of(2024, 9, 30),
            comment = "budget comment",
            currency = Currency.getInstance("EUR"),
            lazyLoader = lazy { BudgetDetails(mutableListOf(expenditure), mutableListOf(spending)) }
        )
        val message = Message(
            id = MessageId(20),
            sender = "Bank",
            text = "You spent 5.00 at Cafe",
            receivedAt = LocalDateTime.of(2024, 1, 1, 9, 59),
            spendingId = SpendingId(10),
            status = Message.Matched(MessageTemplateId(1), 5.0, null),
            isNew = false
        )

        every { mockBudgetRepository.getAll() } returns listOf(budget)
        every { mockMessageRepository.getBySpendingId(SpendingId(10)) } returns message

        val export = testInstance.buildExport()

        assertEquals(1, export.budgets.size)
        with(export.budgets[0]) {
            assertEquals(1, id)
            assertEquals("September 2024", name)
            assertEquals("EUR", currency)
            assertEquals(1, expenditures.size)
            assertEquals(1, spendings.size)

            with(expenditures[0]) {
                assertEquals(1, id)
                assertEquals("Food", name)
                assertEquals(5, iconId)
            }

            with(spendings[0]) {
                assertEquals(10, id)
                assertEquals(1, expenditureId)
                assertEquals("USD", exchangeCurrency)
                assertEquals(1, messages.size)
                assertEquals("Bank", messages[0].sender)
                assertEquals("You spent 5.00 at Cafe", messages[0].text)
            }
        }
    }

    @Test
    fun `importBudgets creates fresh ids, remaps expenditure references and flattens message status`() = runTest {
        every { mockBudgetRepository.getBudgetByName(any()) } returns null
        every { mockBudgetRepository.createBudget() } answers {
            Budget(id = BudgetId(nextId()), lazyLoader = lazy { BudgetDetails(mutableListOf(), mutableListOf()) })
        }
        every { mockExpenditureRepository.create() } answers {
            Expenditure(ExpenditureId(nextId()), "", null, 0.0, 0.0, "", BudgetId(-1), 0)
        }
        every { mockSpendingRepository.create() } answers {
            Spending(
                id = SpendingId(nextId()),
                comment = "",
                date = LocalDateTime.now(),
                amount = 0.0,
                expenditure = Expenditure(ExpenditureId(-1), "", null, 0.0, 0.0, "", BudgetId(-1), 0),
                recurrence = Recurrence.Never
            )
        }
        every { mockMessageRepository.create() } answers {
            Message(id = MessageId(nextId()), sender = "", text = "", status = Message.Rejected)
        }

        val backup = BackupFile(
            exportedAt = LocalDateTime.now(),
            budgets = listOf(
                BudgetBackup(
                    id = 1,
                    name = "September 2024",
                    plan = 100.0,
                    fact = 50.0,
                    startsOn = LocalDate.of(2024, 9, 1),
                    lastDayAt = LocalDate.of(2024, 9, 30),
                    comment = null,
                    currency = "EUR",
                    expenditures = listOf(
                        ExpenditureBackup(id = 11, name = "Food", iconId = null, plan = 100.0, fact = 25.0, comment = "", displayOrder = 0),
                        ExpenditureBackup(id = 12, name = "Transport", iconId = null, plan = 50.0, fact = 25.0, comment = "", displayOrder = 1)
                    ),
                    spendings = listOf(
                        SpendingBackup(
                            id = 21, comment = "Coffee", date = LocalDateTime.of(2024, 9, 2, 10, 0), amount = 5.0,
                            expenditureId = 11, exchangeCurrency = null, exchangeRate = null, exchangeRawAmount = null,
                            recurrence = "Never",
                            messages = listOf(
                                MessageBackup(id = 31, sender = "Bank", text = "Coffee 5.00", receivedAt = LocalDateTime.of(2024, 9, 2, 9, 59), isNew = true)
                            )
                        ),
                        SpendingBackup(
                            id = 22, comment = "Bus", date = LocalDateTime.of(2024, 9, 3, 8, 0), amount = 2.0,
                            expenditureId = 12, exchangeCurrency = null, exchangeRate = null, exchangeRawAmount = null,
                            recurrence = "Never", messages = emptyList()
                        )
                    )
                )
            )
        )

        val summary = testInstance.importBudgets(backup)

        assertEquals(ImportSummary(imported = 1, skipped = 0), summary)

        val savedBudget = budgetSlot.captured
        assertTrue(savedBudget.id.value !in setOf(1, 11, 12, 21, 22, 31)) // fresh id, not an original one
        assertEquals(2, savedBudget.budgetDetails.expenditure.size)
        assertEquals(2, savedBudget.budgetDetails.spending.size)

        val foodExpenditure = savedBudget.budgetDetails.expenditure.first { it.name == "Food" }
        val transportExpenditure = savedBudget.budgetDetails.expenditure.first { it.name == "Transport" }
        assertTrue(foodExpenditure.id.value !in setOf(11, 12))
        assertTrue(transportExpenditure.id.value !in setOf(11, 12))

        val coffeeSpending = savedBudget.budgetDetails.spending.first { it.comment == "Coffee" }
        val busSpending = savedBudget.budgetDetails.spending.first { it.comment == "Bus" }
        assertEquals(foodExpenditure.id, coffeeSpending.expenditure.id)
        assertEquals(transportExpenditure.id, busSpending.expenditure.id)

        assertEquals(1, messageSlots.size)
        val savedMessage = messageSlots[0]
        assertEquals(Message.Recommended, savedMessage.status)
        assertEquals(false, savedMessage.isNew)
        assertEquals(coffeeSpending.id, savedMessage.spendingId)
    }

    @Test
    fun `importBudgets skips a budget whose name already exists`() = runTest {
        every { mockBudgetRepository.getBudgetByName("Existing") } returns mockk(relaxed = true)

        val backup = BackupFile(
            exportedAt = LocalDateTime.now(),
            budgets = listOf(
                BudgetBackup(
                    id = 1, name = "Existing", plan = 0.0, fact = 0.0,
                    startsOn = LocalDate.now(), lastDayAt = LocalDate.now(), comment = null, currency = "EUR",
                    expenditures = emptyList(), spendings = emptyList()
                )
            )
        )

        val summary = testInstance.importBudgets(backup)

        assertEquals(ImportSummary(imported = 0, skipped = 1), summary)
        verify(exactly = 0) { mockBudgetRepository.createBudget() }
        coVerify(exactly = 0) { mockBudgetRepository.save(any()) }
    }

    private fun nextId() = ++nextId
}
