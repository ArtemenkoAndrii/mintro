package com.mas.mobile.service.ai

import android.util.Log
import com.mas.mobile.domain.message.MessageAnalyzer
import com.mas.mobile.domain.message.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GPTMessageAnalyzer @Inject constructor(
    private val gptChatConnector: GptChatConnector
): MessageAnalyzer {

    override suspend fun buildPattern(message: String): Pattern? =
        try {
            ask(message)
        } catch (e: Throwable) {
            Log.e(this::class.java.name, "GPT request failed", e)
            null
        }

    private suspend fun ask(question: String): Pattern? {
        val request = ChatGPTRequest(listOf(
            ChatGPTRequest.Message("system", SYSTEM_CONTENT),
            ChatGPTRequest.Message("user", question)
        ))

        Log.d(this::class.simpleName, "GPT request:\n ${request.messages}")
        val response = gptChatConnector.sendMessage(request)

        return if (response.isSuccessful) {
            val text = response.body()?.choices?.get(0)?.message?.content?.trim() ?: ""
            Log.d(this::class.simpleName, "GPT response:\n $text")
            return Pattern(text)
        } else {
            null
        }
    }

    private companion object {
        const val SYSTEM_CONTENT = "Given a bank SMS, provide a template to parse out the spent amount and merchant name using {amount} and {merchant} placeholders."
    }
}