package com.mas.mobile.repository.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.mas.mobile.domain.budget.SpendingId
import com.mas.mobile.domain.message.Message
import com.mas.mobile.domain.message.MessageId
import com.mas.mobile.domain.message.MessageRepository
import com.mas.mobile.repository.db.config.AppDatabase
import com.mas.mobile.repository.db.entity.SpendingMessage
import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl(val db: AppDatabase) : MessageRepository {
    override fun getById(id: MessageId): Message? =
        db.spendingMessageDao().getById(id.value)?.toModel()

    override fun getBySpendingId(id: SpendingId): Message? =
        db.spendingMessageDao().getBySpendingId(id.value)?.toModel()

    override fun getBySender(name: String): List<Message> =
        db.spendingMessageDao().getBySender(name).map { it.toModel() }

    override fun create(): Message =
        Message(
            id = MessageId(db.idGeneratorDAO().generateId().toInt()),
            sender= "",
            text = "",
            status = Message.Rejected
        )

    override fun getLiveMessages(from: LocalDate): LiveData<List<Message>> =
        db.spendingMessageDao().getLiveMessages(from.atStartOfDay()).map { list ->
            list.map { it.toModel() }.toList()
        }

    override fun countUnreadLive(from: LocalDate): LiveData<Int> =
        db.spendingMessageDao().countUnreadLive()

    override suspend fun save(item: Message) {
        db.spendingMessageDao().upsert(item.toDto())
    }

    override suspend fun remove(item: Message) {
        db.spendingMessageDao().delete(item.toDto())
    }

    private fun SpendingMessage.toModel() = MessageMapper.toModel(this)
    private fun Message.toDto() = MessageMapper.toDTO(this)
}

