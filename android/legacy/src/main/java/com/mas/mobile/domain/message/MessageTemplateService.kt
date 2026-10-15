package com.mas.mobile.domain.message

import com.mas.mobile.domain.settings.SettingsRepository
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageTemplateService @Inject constructor(
    val repository: MessageTemplateRepository,
    private val settingsRepository: SettingsRepository,
    private val messageAnalyzer: MessageAnalyzer,
) {
    suspend fun generateTemplateFromMessage(message: Message): MessageTemplate? {
        val pattern = messageAnalyzer.buildPattern(message.text) ?: return null
        return repository.create().also {
            it.sender = message.sender
            it.pattern = pattern
            it.example = message.text
            it.currency = findCurrency(message.text)
            it.isEnabled = true
        }
    }

    private fun findCurrency(text: String): Currency {
        val defaultCurrency = settingsRepository.get().currency
        val currencies = CurrencyExpert.detectCurrencies(text)
        return when {
            currencies.isEmpty() -> defaultCurrency
            currencies.size == 1 -> currencies[0]
            currencies.contains(defaultCurrency) -> defaultCurrency
            else -> currencies[0]
        }
    }
}