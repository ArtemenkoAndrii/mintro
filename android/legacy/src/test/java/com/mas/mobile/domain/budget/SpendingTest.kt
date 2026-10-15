package com.mas.mobile.domain.budget

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpendingTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(LocalDateTime::class)
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `should exchange amount` () {
        SPENDING.exchange()
        assertEquals(20.00, SPENDING.amount, 0.001)
    }

    /*
        If a scheduled spending was created/updated yesterday or earlier,
        The next execution possible from the beginning of the current day.

        If a scheduled spending was created/updated today,
        The next execution no earlier than tomorrow.

        If a scheduled spending was created/updated next date and later,
        The next execution no earlier than tomorrow.
     */

    @Test
    fun `should calc daily recurrence` () {
        every { LocalDateTime.now() } returns "25.02.2024 12:00".toDateTime()

        // Created yesterday
        val createdYesterdayBeforeTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "24.02.2024 10:00".toDateTime()
        )
        assertEquals(
            "25.02.2024 10:00".toDateTime(),
            createdYesterdayBeforeTime.scheduledDate
        )

        val createdYesterdayAfterTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "24.02.2024 15:00".toDateTime()
        )
        assertEquals(
            "25.02.2024 15:00".toDateTime(),
            createdYesterdayAfterTime.scheduledDate
        )

        // Created today
        val createdTodayBeforeTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "25.02.2024 10:00".toDateTime()
        )
        assertEquals(
            "26.02.2024 10:00".toDateTime(),
            createdTodayBeforeTime.scheduledDate
        )

        val createdTodayAfterTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "25.02.2024 15:00".toDateTime()
        )
        assertEquals(
            "26.02.2024 15:00".toDateTime(),
            createdTodayAfterTime.scheduledDate
        )

        // Created tomorrow
        val createdTomorrowBeforeTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "26.02.2024 10:00".toDateTime()
        )
        assertEquals(
            "26.02.2024 10:00".toDateTime(),
            createdTomorrowBeforeTime.scheduledDate
        )

        val createdTomorrowAfterTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "26.02.2024 15:00".toDateTime()
        )
        assertEquals(
            "26.02.2024 15:00".toDateTime(),
            createdTomorrowAfterTime.scheduledDate
        )
    }

    @Test
    fun `should calc weekly recurrence` () {
        every { LocalDateTime.now() } returns "25.02.2024 12:00".toDateTime()

        val createdYesterdayBeforeTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "24.02.2024 10:00".toDateTime()
        )
        assertEquals(
            "25.02.2024 10:00".toDateTime(),
            createdYesterdayBeforeTime.scheduledDate
        )

        val createdYesterdayAfterTime = SPENDING.copy(
            recurrence = Recurrence.Daily,
            date = "24.02.2024 15:00".toDateTime()
        )
        assertEquals(
            "25.02.2024 15:00".toDateTime(),
            createdYesterdayAfterTime.scheduledDate
        )
    }

    @Test
    fun `should calc monthly recurrence` () {
        every { LocalDateTime.now() } returns "25.02.2024 12:00".toDateTime()

        val createdLastMonth = SPENDING.copy(
            recurrence = Recurrence.Monthly,
            date = "31.01.2024 10:00".toDateTime()
        )

        assertEquals(
            "29.02.2024 10:00".toDateTime(),
            createdLastMonth.scheduledDate
        )
    }

    @Test
    fun `should calc quarterly recurrence` () {
        every { LocalDateTime.now() } returns "01.04.2024 12:00".toDateTime()

        val createdLastMonth = SPENDING.copy(
            recurrence = Recurrence.Monthly,
            date = "01.01.2024 12:00".toDateTime()
        )

        assertEquals(
            "01.04.2024 12:00".toDateTime(),
            createdLastMonth.scheduledDate
        )
    }

    @Test
    fun `should calc never recurrence` () {
        val value = LocalDateTime.of(2024, 1, 31, 15, 0, 0)
        val dailySpending = SPENDING.copy(date = value, recurrence = Recurrence.Never)
        assertNull(dailySpending.scheduledDate)
    }

    private companion object {
        val EXPENDITURE = Expenditure(
            ExpenditureId(2),
            "Food",
            null,
            100.00,
            0.00,
            "",
            BudgetId(0),
            0
        )

        val SPENDING = Spending(
            SpendingId(1),
            "McDonalds",
            LocalDateTime.now(),
            0.00,
            EXPENDITURE,
            exchangeInfo = ExchangeInfo(
                rawAmount = 10.00,
                rate = 2.00,
                currency = Currency.getInstance("EUR")
            ),
            recurrence = Recurrence.Never
        )
    }

    private fun String.toDateTime(): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return LocalDateTime.parse(this, formatter)
    }

    private fun String.toDate(): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return LocalDate.parse(this, formatter)
    }
}