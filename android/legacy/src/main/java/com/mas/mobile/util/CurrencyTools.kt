package com.mas.mobile.util

import java.util.Currency
import java.util.Locale

object CurrencyTools {
    fun getSystemCurrency(): Currency {
        val local = Locale.getDefault()

        return try {
            Currency.getInstance(local)
        } catch (e: Exception) {
            if (local.language == "en") {
                Currency.getInstance("USD")
            } else {
                Currency.getInstance("EUR")
            }
        }
    }
}