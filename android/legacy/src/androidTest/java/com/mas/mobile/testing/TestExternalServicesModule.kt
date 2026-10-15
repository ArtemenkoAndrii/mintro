package com.mas.mobile.testing

import com.mas.mobile.ExternalServicesModule
import com.mas.mobile.domain.budget.ExchangeRepository
import com.mas.mobile.domain.message.MessageAnalyzer
import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.TaskService
import com.mas.mobile.service.ai.GptChatConnector
import dagger.Provides
import javax.inject.Singleton

/**
 * Instrumented-test replacement for [ExternalServicesModule]. Swaps the real GPT/currency
 * API clients and the real background dispatcher for deterministic in-process fakes, so UI
 * tests never make a live network call and never race a genuine background thread.
 */
class TestExternalServicesModule : ExternalServicesModule() {
    @Provides
    @Singleton
    override fun resolveExchangeRepository(): ExchangeRepository = FakeExchangeRepository()

    // Dagger still resolves this to satisfy resolveMessageProcessor's signature below, but it's
    // never actually invoked since resolveMessageProcessor ignores it and returns a fixed fake.
    @Provides
    @Singleton
    override fun resolveGptChatService(): GptChatConnector = FakeGptChatConnector()

    @Provides
    @Singleton
    override fun resolveMessageProcessor(gptChatConnector: GptChatConnector): MessageAnalyzer = FakeMessageAnalyzer()

    @Provides
    @Singleton
    override fun resolveTaskService(coroutineService: CoroutineService): TaskService = SynchronousTaskService()
}
