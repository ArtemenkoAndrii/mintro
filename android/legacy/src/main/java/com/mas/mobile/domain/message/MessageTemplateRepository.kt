package com.mas.mobile.domain.message

import androidx.lifecycle.LiveData
import com.mas.mobile.domain.Repository
import com.mas.mobile.domain.budget.CategoryLiveData

interface MessageTemplateRepository : Repository<MessageTemplate> {
    val live: MessageTemplateLiveData
    fun getById(id: MessageTemplateId): MessageTemplate?
    fun getAll(): List<MessageTemplate>
    fun create(): MessageTemplate
    fun clone(messageTemplate: MessageTemplate): MessageTemplate
}

interface MessageTemplateLiveData {
    fun getAll(): LiveData<List<MessageTemplate>>
}
