package com.mas.mobile.domain.message

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Currency

class MessageTemplateTest {
    @Test
    fun `should parse`() {
        val result = MESSAGE_TEMPLATE.parse(TEXT)

        with (result as MessageTemplate.Result) {
            assertEquals(4.10, result.amount, 0.001)
            assertEquals("aliexpress", result.merchant!!.value)
        }
    }

    @Test
    fun `should return null`() {
        assertNull(MESSAGE_TEMPLATE.parse("Any random text"))
    }

    private companion object {
        const val SENDER = "BBVA"
        const val TEXT = "Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted"
        val EUR = Currency.getInstance("EUR")
        val PATTERN = Pattern("Payment of {amount} EUR in {merchant} with")
        val MESSAGE_TEMPLATE = MessageTemplate(
            id = MessageTemplateId(0),
            sender = SENDER,
            pattern = PATTERN,
            example = TEXT,
            currency = EUR,
            isEnabled = true
        )
    }
}