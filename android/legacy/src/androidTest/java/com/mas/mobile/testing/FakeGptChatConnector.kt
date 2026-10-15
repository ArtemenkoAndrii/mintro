package com.mas.mobile.testing

import com.mas.mobile.service.ai.ChatGPTRequest
import com.mas.mobile.service.ai.ChatGPTResponse
import com.mas.mobile.service.ai.GptChatConnector
import retrofit2.Response

/**
 * No-op [GptChatConnector] for instrumented tests. Dagger still needs to resolve a
 * `GptChatConnector` to satisfy [com.mas.mobile.ExternalServicesModule.resolveMessageProcessor]'s
 * signature, but [FakeMessageAnalyzer] replaces `MessageAnalyzer` itself and never touches this
 * connector, so `sendMessage` is never expected to actually run in a test.
 */
class FakeGptChatConnector : GptChatConnector {
    override suspend fun sendMessage(request: ChatGPTRequest): Response<ChatGPTResponse> =
        throw UnsupportedOperationException(
            "GptChatConnector.sendMessage should not be called in instrumented tests - MessageAnalyzer is faked."
        )
}
