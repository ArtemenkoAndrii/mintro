package com.mas.mobile.domain.budget

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Currency

data class Spending(
    val id: SpendingId,
    var comment: String,
    var date: LocalDateTime,
    var amount: Double,
    var expenditure: Expenditure,
    var exchangeInfo: ExchangeInfo? = null,
    var recurrence: Recurrence
) {
    val scheduledDate: LocalDateTime?
        get() = calcScheduledDate()

    fun exchange() = exchangeInfo?.let { method ->
        amount = BigDecimal(method.rawAmount * method.rate)
            .setScale(2, RoundingMode.HALF_EVEN)
            .toDouble()
    }

    private fun calcScheduledDate(): LocalDateTime? {
        // Mockk can't mock LocalDate.now(), so replace after tests run
        val now = LocalDateTime.now().toLocalDate()
        val limit = if (this.date.toLocalDate() == now) {
            // created today
            now.atTime(LocalTime.MAX)
        } else {
            // past or future
            now.atStartOfDay()
        }

        return generateSequence(this.date) { it.increment() }.firstOrNull { it >= limit }
    }

    private fun LocalDateTime.increment() =
        when (recurrence) {
            Recurrence.Daily -> this.plusDays(1)
            Recurrence.Weekly -> this.plusWeeks(1)
            Recurrence.Monthly -> this.plusMonths(1)
            Recurrence.Quarterly -> this.plusMonths(3)
            else -> null
        }
}

data class ExchangeInfo(
    var rawAmount: Double,
    var rate: Double,
    var currency: Currency
)

@JvmInline
value class SpendingId(val value: Int)

enum class Recurrence {
    Never, Daily, Weekly, Monthly, Quarterly
}