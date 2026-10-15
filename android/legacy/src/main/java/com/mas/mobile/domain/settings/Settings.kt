package com.mas.mobile.domain.settings

import com.mas.mobile.util.CurrencyTools
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

data class Settings(
    var period: Period = Period.MONTH,
    var currency: Currency = CurrencyTools.getSystemCurrency(),
    var startDayOfMonth: DayOfMonth = LocalDate.now().startDayOfMonth(),
    var startDayOfWeek: DayOfWeek = LocalDate.now().startDayOfWeek(),
    var captureSms: Boolean = false,
    var captureNotifications: Boolean = false,
    var policyVersion: String = "",
    var discoveryMode: Boolean = true
)

fun LocalDate.startDayOfWeek(): DayOfWeek = this.dayOfWeek
fun LocalDate.startDayOfMonth(): DayOfMonth {
    val result = if (this.dayOfMonth > 28) {
        28
    } else {
        this.dayOfMonth
    }
    return DayOfMonth(result)
}

enum class Period {
    MONTH, WEEK, TWO_WEEKS, QUARTER, YEAR
}

@JvmInline
value class DayOfMonth(val value: Int) {
    init {
        require(value in 1..31)
    }
}
