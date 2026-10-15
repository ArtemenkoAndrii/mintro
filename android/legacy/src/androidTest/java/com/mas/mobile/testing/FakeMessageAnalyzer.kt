package com.mas.mobile.testing

import com.mas.mobile.domain.message.MessageAnalyzer
import com.mas.mobile.domain.message.Pattern

/**
 * Deterministic [MessageAnalyzer] for instrumented tests - never calls the real GPT API.
 * Returns [pattern] for every message (default: a fixed amount-only pattern), so tests
 * exercising `MessageService.promoteRecommendedMessage` don't depend on a live network call.
 */
class FakeMessageAnalyzer(private val pattern: Pattern? = Pattern.SIMPLE) : MessageAnalyzer {
    override suspend fun buildPattern(message: String): Pattern? = pattern
}
