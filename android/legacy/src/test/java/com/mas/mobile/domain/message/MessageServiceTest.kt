package com.mas.mobile.domain.message

import android.util.Log
import com.mas.mobile.DummyEventLogger
import com.mas.mobile.DummyTaskService
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.CategoryRepository
import com.mas.mobile.domain.budget.CategoryService
import com.mas.mobile.domain.budget.ExchangeRepository
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.domain.budget.ExpenditureRepository
import com.mas.mobile.domain.budget.Recurrence
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.domain.budget.SpendingId
import com.mas.mobile.domain.budget.SpendingRepository
import com.mas.mobile.domain.settings.SettingsRepository
import com.mas.mobile.domain.settings.SettingsService
import com.mas.mobile.service.ErrorHandler
import com.mas.mobile.service.ResourceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Currency

class MessageServiceTest {
    private val mockQualifierService = mockk<QualifierService>(relaxed = true)
    private val mockMessageRepository = mockk<MessageRepository>(relaxed = true)
    private val mockExchangeRepository = mockk<ExchangeRepository>(relaxed = true)
    private val mockCategoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val mockMessageTemplateService = mockk<MessageTemplateService>(relaxed = true)
    private val mockMessageTemplateRepository = mockk<MessageTemplateRepository>(relaxed = true)
    private val mockCategoryService = mockk<CategoryService>(relaxed = true)
    private val mockSettingsService = mockk<SettingsService>(relaxed = true)

    // BudgetService mockk doesn't work because of bug with mocking Int value classes
    private val budgetService = BudgetService(
        mockk<ResourceService>(relaxed = true),
        mockk<SettingsRepository>(relaxed = true),
        mockk<SpendingRepository>(relaxed = true) {
            every { create() } returns Spending(
                SPENDING_ID, "", TIMESTAMP, 0.0, mockk<Expenditure>(relaxed = true), null, Recurrence.Never
            )
        },
        mockExchangeRepository,
        mockCategoryRepository,
        mockk<ErrorHandler>(relaxed = true),
        DummyTaskService,
        DummyEventLogger,
        mockk<BudgetRepository>(relaxed = true),
        mockk<ExpenditureRepository>(relaxed = true)
    )

    private val message = slot<Message>()

    private val testInstance = MessageService(
        DummyTaskService,
        mockMessageTemplateService,
        mockCategoryService,
        budgetService,
        mockQualifierService,
        mockSettingsService,
        DummyEventLogger,
        mockMessageRepository
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0

        every { mockMessageTemplateService.repository } returns mockMessageTemplateRepository
        every { mockMessageTemplateRepository.getAll() } returns listOf(TEMPLATE)
        every { mockMessageRepository.create() } returns NEW_MESSAGE
        every { mockQualifierService.isRecommended(any(), any()) } returns true
        every { mockSettingsService.autodetect } returns true
        every { mockMessageRepository.getBySender(any()) } returns listOf(RECOMMENDED_MESSAGE.copy())

        coEvery { mockMessageRepository.save(capture(message)) } returns Unit
        coEvery { mockExchangeRepository.getRate(any(), any()) } returns Result.success(RATE)
        coEvery { mockMessageTemplateService.generateTemplateFromMessage(any()) } returns TEMPLATE
    }

    @AfterEach
    fun afterEach() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `should handle raw message as matched`() {
        testInstance.handleRawMessage(
            SENDER,
            TEXT,
            TIMESTAMP
        )

        with(message.captured) {
            assertEquals(SENDER, sender)
            assertEquals(TEXT, text)
            assertTrue(isNew)
            with(status as Message.Matched) {
                assertEquals(3.77, amount, 0.001)
                assertEquals("AliExpress", merchant!!.value)
            }
        }
    }

    @Test
    fun `should handle raw message as recommended`() {
        every { mockMessageTemplateRepository.getAll() } returns emptyList()

        testInstance.handleRawMessage(
            SENDER,
            TEXT,
            TIMESTAMP
        )

        with(message.captured) {
            assertEquals(SENDER, sender)
            assertEquals(TEXT, text)
            assertTrue(isNew)
            assertTrue(status is Message.Recommended)
        }
    }

    @Test
    fun `should handle raw message as rejected`() {
        every { mockMessageTemplateRepository.getAll() } returns emptyList()
        every { mockQualifierService.isRecommended(any(), any()) } returns false

        testInstance.handleRawMessage(
            SENDER,
            TEXT,
            TIMESTAMP
        )

        assertFalse(message.isCaptured)
    }

    @Test
    fun `should promoted to matched`() = runTest {
        val result = testInstance.promoteRecommendedMessage(RECOMMENDED_MESSAGE)

        coVerify { mockMessageTemplateRepository.save(TEMPLATE) }

        with(result!!.status as Message.Matched) {
            assertEquals(3.77, amount, 0.001)
            assertEquals("AliExpress", merchant!!.value)
        }
    }

    private companion object {
        const val RATE: Double = 1.00
        const val SENDER = "Revolut"
        const val TEXT = "AliExpress 🛍 Paid €3.77 at AliExpress Spent today: €100.00."
        val TIMESTAMP: LocalDateTime = LocalDateTime.now()
        val SPENDING_ID: SpendingId = SpendingId(0)
        val CURRENCY = Currency.getInstance("EUR")

        val NEW_MESSAGE = Message(
            id = MessageId(0),
            sender = "",
            text = "",
            status = Message.Rejected
        )
        val RECOMMENDED_MESSAGE = Message(
            id = MessageId(0),
            sender = SENDER,
            text = TEXT,
            status = Message.Recommended
        )
        val TEMPLATE = MessageTemplate(
            id = MessageTemplateId(0),
            sender = SENDER,
            pattern = Pattern("Paid €{amount} at {merchant} Spent"),
            example = TEXT,
            currency = CURRENCY,
            isEnabled = true
        )
    }
}
