package com.mas.mobile.service.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Singleton

@Singleton
interface GptChatConnector {
    @POST("/v1/chat/completions")
    suspend fun sendMessage(@Body request: ChatGPTRequest): Response<ChatGPTResponse>
}

data class ChatGPTRequest(
    val messages: List<Message>,
    val model: String = "ft:gpt-3.5-turbo-1106:personal:patterns:9TUF7TfU",
    val temperature: Double = 0.2
) {
    data class Message(
        val role: String,
        val content: String
    )
}

data class ChatGPTResponse(
    val id: String,
    val usage: Usage,
    val choices: List<Choice>
) {
    data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )

    data class Choice(
        val message: Message,
        val finish_reason: String,
        val index: Int
    )

    data class Message(
        val role: String,
        val content: String
    )
}
