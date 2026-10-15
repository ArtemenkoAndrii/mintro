package com.mas.mobile.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import kotlin.test.assertEquals

class CurrencyToolsTest {
    private val default: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        Locale.setDefault(default)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(default)
    }

    @Test
    fun `should return default currency for country local`() {
        Locale.setDefault(Locale.UK)
        val currency = CurrencyTools.getSystemCurrency()
        assertEquals(Currency.getInstance("GBP"), currency)
    }

    @Test
    fun `should return default currency for lang`() {
        Locale.setDefault(Locale("en", "XX"))
        var currency = CurrencyTools.getSystemCurrency()
        assertEquals(Currency.getInstance("USD"), currency)

        Locale.setDefault(Locale("es", "XX"))
        currency = CurrencyTools.getSystemCurrency()
        assertEquals(Currency.getInstance("EUR"), currency)
    }
}