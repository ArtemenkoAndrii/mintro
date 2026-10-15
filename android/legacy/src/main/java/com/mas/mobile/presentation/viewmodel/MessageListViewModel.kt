package com.mas.mobile.presentation.viewmodel

import com.mas.mobile.domain.budget.SpendingId
import com.mas.mobile.domain.budget.SpendingRepository
import com.mas.mobile.domain.message.Message
import com.mas.mobile.domain.message.MessageRepository
import com.mas.mobile.domain.message.QualifierService
import com.mas.mobile.domain.settings.SettingsService
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.LocalDate

class MessageListViewModel @AssistedInject constructor(
    private val messageRepository: MessageRepository,
    private val coroutineService: CoroutineService,
    private val spendingRepository: SpendingRepository,
    private val qualifierService: QualifierService,
    settings: SettingsService,
    @Assisted param: String
): ListViewModel<Message>(coroutineService, messageRepository) {
    private var suggestEnableCapturing = !settings.isMessageCapturingEnabled()
    val messages = messageRepository.getLiveMessages(
        LocalDate.now().minusDays(30)
    )

    fun markAsRead(item: Message) {
        coroutineService.backgroundTask {
            item.isNew = false
            messageRepository.save(item)
        }
    }

    fun getSpending(spendingId: SpendingId) = spendingRepository.getSpending(spendingId)

    fun suggestEnableCapturing() =
        suggestEnableCapturing.also { suggestEnableCapturing = false }

    fun blacklist(item: Message) {
        qualifierService.addToBlacklist(item.sender)
        coroutineService.backgroundTask {
            messageRepository.remove(item)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(param: String): MessageListViewModel
    }
}
