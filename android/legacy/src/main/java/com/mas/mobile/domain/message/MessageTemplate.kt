package com.mas.mobile.domain.message

import com.mas.mobile.domain.budget.Merchant
import java.util.Currency

data class MessageTemplate(
    val id: MessageTemplateId,
    var sender: String,
    var pattern: Pattern,
    var example: String,
    var currency: Currency,
    var isEnabled: Boolean
) {
    fun parse(text: String): Result? =
        when (val result = pattern.parse(text)) {
            is Pattern.Data -> Result(
                result.amount,
                result.merchant?.let { Merchant(it) }
            )
            else -> null
        }

    data class Result(
        val amount: Double,
        val merchant: Merchant?
    )
}

@JvmInline
value class MessageTemplateId(val value: Int)
