package com.mas.mobile.service.ai

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

class GPTMessageAnalyzerTest {
    private val mockGptChatConnector = mockk<GptChatConnector>()
    private val mockResponse = mockk<Response<ChatGPTResponse>>()
    private val mockGptChatResponse = mockk<ChatGPTResponse>()

    private val requestSlot = slot<ChatGPTRequest>()

    private val underTest = GPTMessageAnalyzer(mockGptChatConnector)

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns mockGptChatResponse
        every { mockGptChatResponse.choices } returns listOf(
            ChatGPTResponse.Choice(
                message = ChatGPTResponse.Message(
                    role = SYSTEM,
                    content = EXPECTED_RESPONSE
                ),
                finish_reason = CODE,
                index = 1
        ))

        coEvery { mockGptChatConnector.sendMessage(any()) } returns mockResponse
    }

    @Test
    fun `should send valid request`() = runTest {
        val result = underTest.buildPattern(MESSAGE)

        coVerify(exactly = 1) { mockGptChatConnector.sendMessage(capture(requestSlot)) }
        with(requestSlot.captured.messages[0]) {
            assertEquals(SYSTEM, role)
            assertEquals(SYSTEM_MESSAGE, content)
        }
        with(requestSlot.captured.messages[1]) {
            assertEquals(USER, role)
            assertEquals(MESSAGE, content)
        }
        assertEquals(EXPECTED_RESPONSE, result?.value)
    }

    @Test
    fun `should handle unexpected response`() = runTest {
        every { mockGptChatResponse.choices } returns listOf(
            ChatGPTResponse.Choice(
                message = ChatGPTResponse.Message(
                    role= SYSTEM,
                    content = UNEXPECTED_RESPONSE
                ),
                finish_reason = CODE,
                index = 1
            ))

        val result = underTest.buildPattern(MESSAGE)
        assertNull(result?.value)
    }

    private companion object {
        const val SYSTEM = "system"
        const val USER = "user"
        const val CODE = "OK"
        const val MESSAGE = "Payment accepted! Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted."
        const val SYSTEM_MESSAGE = "Given a bank SMS, provide a template to parse out the spent amount and merchant name using {amount} and {merchant} placeholders."
        const val EXPECTED_RESPONSE = "of {amount} EUR in {merchant} with"
        const val UNEXPECTED_RESPONSE = "Thank you for confirming the payment. If you have any further questions or need assistance with your AliExpress transaction, feel free to ask!"
    }
}