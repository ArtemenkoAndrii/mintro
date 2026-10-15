package com.mas.mobile.domain.message

import androidx.lifecycle.LiveData
import com.mas.mobile.domain.Repository
import com.mas.mobile.domain.budget.SpendingId
import java.time.LocalDate

interface MessageRepository : Repository<Message> {
    fun getById(id: MessageId): Message?
    fun getBySpendingId(id: SpendingId): Message?
    fun getBySender(name: String): List<Message>
    fun create(): Message

    fun getLiveMessages(from: LocalDate): LiveData<List<Message>>
    fun countUnreadLive(from: LocalDate): LiveData<Int>
}