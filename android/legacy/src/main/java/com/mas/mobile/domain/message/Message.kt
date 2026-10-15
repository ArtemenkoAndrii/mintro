package com.mas.mobile.domain.message

import com.mas.mobile.domain.budget.Merchant
import com.mas.mobile.domain.budget.SpendingId
import java.time.LocalDateTime

data class Message(
    val id: MessageId,
    var sender: String,
    var text: String,
    var receivedAt: LocalDateTime = LocalDateTime.now(),
    var spendingId: SpendingId? = null,
    var status: Status,
    var isNew: Boolean = true
) {
    fun hasSpending() = spendingId != null
    fun getAmount(): Double? = (status as? Matched)?.amount
    fun getMerchant(): Merchant? = (status as? Matched)?.merchant

    sealed class Status
    object Recommended: Status()
    object Rejected: Status()
    data class Matched(
        val messageTemplateId: MessageTemplateId,
        val amount: Double,
        val merchant: Merchant?
    ): Status()
}

@JvmInline
value class MessageId(val value: Int)