package com.mas.mobile.domain.message

import android.util.Log

data class Pattern(val value: String = AMOUNT_PLACEHOLDER) {
    init {
        val hasAmount = value.contains(AMOUNT_PLACEHOLDER, ignoreCase = true)
        require(hasAmount) { "The pattern must have $AMOUNT_PLACEHOLDER placeholder" }
        require(value.length <= MAX_LENGTH) { "The value exceeds the maximum length of $MAX_LENGTH" }
    }

    fun parse(message: String): Result {
        val amountIndex = value.indexOf(AMOUNT_PLACEHOLDER, ignoreCase = true)
        if (amountIndex == -1) {
            return Empty
        }

        val merchantIndex = value.indexOf(MERCHANT_PLACEHOLDER, ignoreCase = true)

        return value.find(message).toResult(amountFirst = amountIndex < merchantIndex)
            ?: value.cutToAmount().find(message).toResult()
            ?: return Empty
    }

    private fun String.find(text: String) = try {
        this.escapeSpecialCharacters()
            .replacePlaceholder(AMOUNT_PLACEHOLDER, AMOUNT_REGEX)
            .replacePlaceholder(MERCHANT_PLACEHOLDER, MERCHANT_REGEX)
            .let { Regex(ANY_STRING_REGEX + it + ANY_STRING_REGEX) }
            .find(text.escapeNewLines())
    } catch (e: Throwable) {
        Log.i(this::class.java.name, "Matcher regex failed.", e)
        null
    }

    private fun String.escapeSpecialCharacters(): String {
        val specialChars = mutableSetOf('.', '$', '^', '[', ']', '(', ')', '|', '*', '+', '?', '\\')
        return this.map { char ->
            if (char in specialChars) "\\$char" else char.toString()
        }.joinToString("")
    }

    private fun String.escapeNewLines() = this
        .replace(Regex("\\r\\n|\\r|\\n"), SINGLE_SPACE)
        .replace(Regex("\\s+"), SINGLE_SPACE)

    private fun String.replacePlaceholder(placeholder: String, regex: String) =
        when {
            this.startsWith(placeholder) -> START_STRING + this.replace(placeholder, regex, true)
            this.endsWith(placeholder) -> this.replace(placeholder, regex, true) + END_STRING
            else -> this.replace(placeholder, regex, true)
        }

    private fun String.cutToAmount(): String {
        val amountIndex = this.indexOf(AMOUNT_PLACEHOLDER)
        val merchantIndex = this.indexOf(MERCHANT_PLACEHOLDER)

        return if (amountIndex < merchantIndex) {
            this.substring(0, merchantIndex - 1)
        } else {
            this.substring(merchantIndex + MERCHANT_PLACEHOLDER.length,  this.length)
        }
    }

    private fun MatchResult?.toResult(amountFirst: Boolean = true) = this?.let { result ->
        when (result.groups.size) {
            2 -> respondAmount(result.groups[1]!!)
            3 -> if (amountFirst) {
                    respondAmountAndMerchant(result.groups[1]!!, result.groups[2]!!)
                 } else {
                    respondAmountAndMerchant(result.groups[2]!!, result.groups[1]!!)
                 }
            else -> null
        }
    }

    private fun respondAmount(amount: MatchGroup) =
        Data(
            amount.value.asDouble(),
            null,
            Indexes(amount.range.first, amount.range.last, -1, -1)
        )

    private fun respondAmountAndMerchant(amount: MatchGroup, merchant: MatchGroup) =
        Data(
            amount.value.asDouble(),
            merchant.value,
            Indexes(amount.range.first, amount.range.last, merchant.range.first, merchant.range.last)
        )

    private fun String?.asDouble() = this?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

    companion object {
        val SIMPLE = Pattern("{amount}")
        private const val MAX_LENGTH = 100
        private const val AMOUNT_PLACEHOLDER = "{amount}"
        private const val MERCHANT_PLACEHOLDER = "{merchant}"
        private const val ANY_STRING_REGEX = ".*"
        private const val AMOUNT_REGEX = "(\\d+(?:[.,]\\d+)?)"
        private const val MERCHANT_REGEX = "($ANY_STRING_REGEX)"
        private const val START_STRING = "^"
        private const val END_STRING = "$"
        private const val SINGLE_SPACE = " "
    }

    sealed interface Result
    data class Data(val amount: Double, val merchant: String?, val indexes: Indexes) : Result
    data class Indexes(
        val amountStart: Int,
        val amountEnd: Int,
        val merchantStart: Int,
        val merchantEnd: Int
    )
    object Empty : Result
}
