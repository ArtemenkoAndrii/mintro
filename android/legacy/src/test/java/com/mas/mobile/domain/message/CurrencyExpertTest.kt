package com.mas.mobile.domain.message

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Currency

class CurrencyExpertTest {
    @Test
    fun `should find EUR by code`() {
        val result = CurrencyExpert.detectCurrencies("Payment accepted 👍💳 Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted.")
        assertEquals("EUR", result[0].currencyCode)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find EUR by symbol`() {
        val result = CurrencyExpert.detectCurrencies("Paid €3.77 at AliExpress Spent today: €100.00.")
        assertEquals("EUR", result[0].currencyCode)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find USD by code at new line`() {
        val result = CurrencyExpert.detectCurrencies("BBVA\nPaid 200,00 USD in Parki with your card ending in 1234 accepted!...")
        assertEquals("USD", result[0].currencyCode)
        assertEquals(1, result.size)
    }

    @Test
    fun `should find a few currencies`() {
        val result = CurrencyExpert.detectCurrencies("Paid €3.77 at AliExpress Spent today: 100.00 USD")
        assertTrue(result.contains(Currency.getInstance("EUR")))
        assertTrue(result.contains(Currency.getInstance("USD")))
        assertEquals(2, result.size)
    }
}