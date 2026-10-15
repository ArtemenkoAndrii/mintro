package com.mas.mobile.repository.message

import com.mas.mobile.domain.message.MessageTemplate
import com.mas.mobile.domain.message.MessageTemplateId
import com.mas.mobile.domain.message.Pattern
import com.mas.mobile.toCurrency
import com.mas.mobile.repository.db.entity.MessageTemplate as MessageTemplateData

object MessageTemplateMapper {
    fun toModel(dto: MessageTemplateData) =
        MessageTemplate(
            id = MessageTemplateId(dto.id),
            sender = dto.sender,
            pattern = Pattern(dto.pattern),
            example = dto.example,
            currency = dto.currency.toCurrency(),
            isEnabled = dto.isEnabled
        )

    fun toDTO(model: MessageTemplate) =
        MessageTemplateData(
            id = model.id.value,
            sender = model.sender,
            pattern = model.pattern.value,
            example = model.example,
            currency = model.currency.currencyCode,
            isEnabled = model.isEnabled
        )
}