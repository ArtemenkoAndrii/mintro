package com.mas.mobile.domain.message

import com.mas.mobile.domain.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Currency

class MessageTemplateServiceTest {
    private val mockMessageTemplateRepository = mockk<MessageTemplateRepository>(relaxed = true)
    private val mockSettingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mockMessageAnalyzer = mockk<MessageAnalyzer>(relaxed = true)

    private val underTest = MessageTemplateService(mockMessageTemplateRepository, mockSettingsRepository, mockMessageAnalyzer)

    @BeforeEach
    fun setUp() {
        coEvery { mockMessageAnalyzer.buildPattern(any()) } returns PATTERN
        every { mockMessageTemplateRepository.create() } returns MESSAGE_TEMPLATE
    }

    @Test
    fun `should generate template`() = runTest {
        val result = underTest.generateTemplateFromMessage(MESSAGE)

        with(result!!) {
            assertEquals(SENDER, sender)
            assertEquals(PATTERN, pattern)
            assertEquals(TEXT, example)
            assertEquals(EUR, currency)
            assertTrue(isEnabled)
        }
    }

    @Test
    fun `should return null`() = runTest {
        coEvery { mockMessageAnalyzer.buildPattern(any()) } returns null
        assertNull(underTest.generateTemplateFromMessage(MESSAGE))
    }

    private companion object {
        const val SENDER = "BBVA"
        const val TEXT = "Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted"
        val EUR = Currency.getInstance("EUR")

        val MESSAGE = Message(
            id = MessageId(0),
            sender = SENDER,
            text = TEXT,
            receivedAt = LocalDateTime.now(),
            spendingId = null,
            status = Message.Recommended,
            isNew = true
        )

        val PATTERN = Pattern("{amount}")

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