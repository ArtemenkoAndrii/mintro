package com.mas.mobile.domain.message

import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.TaskService
import com.mas.mobile.util.CurrencyTools
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualifierService @Inject constructor(
    private val coroutineService: TaskService,
    private val repository: QualifierRepository
) {
    fun isRecommended(sender: String, text: String): Boolean {
        val lowerText = text.lowercase()
        return (lowerText.length < MESSAGE_LIMIT)
                && !isInBlackList(sender)
                && containsAmount(lowerText)
                && containsCurrency(lowerText)
                && matchesKeywords(lowerText)
    }

    fun addToBlacklist(sender: String) = coroutineService.backgroundTask {
        val blacklisted = repository.create().also {
            it.type = Qualifier.Type.BLACKLIST
            it.value = sender.trim()
        }
        repository.save(blacklisted)
    }

    private fun isInBlackList(sender: String): Boolean =
        repository.getQualifiers(Qualifier.Type.BLACKLIST)
            .any { it.value.equals(sender, true) }

    private fun containsAmount(text: String) = AMOUNT_REGEX.containsMatchIn(text)

    private fun containsCurrency(text: String) =
        CURRENCIES.entries.any {
            text.contains(it.key, ignoreCase = true) || text.contains(it.value, ignoreCase = true)
        }

    private fun matchesKeywords(text: String): Boolean {
        val skipWords = repository.getQualifiers(Qualifier.Type.SKIP).map { it.value.lowercase() }
        if (skipWords.any { text.contains(it, true) }) {
            return false
        }

        val catchWords = repository.getQualifiers(Qualifier.Type.CATCH).map { it.value.lowercase() }
        return catchWords.any { text.contains(it, true) }
    }

    companion object {
        const val MESSAGE_LIMIT = 250

        val AMOUNT_REGEX = Regex(".*\\d+[.,]\\d+?.*")
        val WORDS_REGEX = "\\s+|\\p{Punct}".toRegex()

        val CURRENCIES = mutableMapOf(
            "UAH" to "₴",
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "JPY" to "¥",
            "AUD" to "$",
            "CAD" to "$",
            "CHF" to "Fr",
            "CNY" to "¥",
            "RUB" to "₽"
        ).also {
            with(CurrencyTools.getSystemCurrency()) {
                it[currencyCode.uppercase()] = symbol
            }
        }
    }
}