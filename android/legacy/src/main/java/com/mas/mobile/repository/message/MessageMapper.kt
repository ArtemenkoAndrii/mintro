package com.mas.mobile.repository.message

import com.mas.mobile.domain.budget.Merchant
import com.mas.mobile.domain.budget.SpendingId
import com.mas.mobile.domain.message.Message
import com.mas.mobile.domain.message.MessageId
import com.mas.mobile.domain.message.MessageTemplateId
import com.mas.mobile.repository.db.entity.SpendingMessage

object MessageMapper {
    fun toModel(dto: SpendingMessage) =
        Message(
            id = MessageId(dto.id),
            sender = dto.name,
            text = dto.message,
            receivedAt = dto.receivedAt,
            spendingId = dto.spendingId?.let { SpendingId(it) },
            status = if (dto.status == SpendingMessage.MATCHED) {
                        Message.Matched(
                            messageTemplateId = MessageTemplateId(dto.ruleId),
                            amount = dto.suggestedAmount,
                            merchant = dto.suggestedMerchant?.let { Merchant(it) }
                        )
                     } else {
                        Message.Recommended
                     },
            isNew = dto.isNew
        )

    fun toDTO(model: Message) =
        with (model.status as? Message.Matched) {
            SpendingMessage(
                id = model.id.value,
                name = model.sender,
                message = model.text,
                receivedAt = model.receivedAt,
                status = if (this != null) SpendingMessage.MATCHED else SpendingMessage.RECOMMENDED,
                ruleId = this?.messageTemplateId?.value ?: -1,
                suggestedMerchant = this?.merchant?.value,
                suggestedAmount = this?.amount ?: 0.0,
                spendingId = model.spendingId?.value,
                isNew = model.isNew,
            )
        }
}