package com.mas.mobile.domain.message

import android.util.Log
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.MessageEvaluated
import com.mas.mobile.domain.analytics.MessageTemplateFailed
import com.mas.mobile.domain.analytics.MessageTemplateFailed.Status
import com.mas.mobile.domain.budget.BudgetService
import com.mas.mobile.domain.budget.CategoryService
import com.mas.mobile.domain.settings.SettingsService
import com.mas.mobile.service.TaskService
import java.time.LocalDateTime
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageService @Inject constructor(
    private val coroutineService: TaskService,
    private val messageTemplateService: MessageTemplateService,
    private val categoryService: CategoryService,
    private val budgetService: BudgetService,
    private val qualifierService: QualifierService,
    private val settings: SettingsService,
    private val eventLogger: EventLogger,
    val repository: MessageRepository
) {
    fun handleRawMessage(sender: String, text: String, date: LocalDateTime) = coroutineService.backgroundTask {
        val status = assessStatus(sender, text)
        logEvent(status)

        if (status !is Message.Rejected) {
            val message = repository.create().also {
                it.sender = sender
                it.text = text
                it.receivedAt = date
                it.status = status
                it.isNew = true
            }

            if (status is Message.Matched) {
                createSpending(message)
            }

            repository.save(message)
        } else {
            Log.i(this::class.simpleName, "A message from $sender doesn't match any rule.")
        }
    }

    fun getCurrency(message: Message): Currency? {
        val status = message.status as? Message.Matched ?: return null
        return messageTemplateService.repository.getById(status.messageTemplateId)?.currency
    }

    suspend fun promoteRecommendedMessage(message: Message): Message? {
        if (message.status !is Message.Recommended) {
            return null
        }

        val newTemplate = messageTemplateService.generateTemplateFromMessage(message)
        if (newTemplate == null) {
            eventLogger.log(MessageTemplateFailed(Status.Generation))
            return null
        }

        val newStatus = matchesTemplate(message.sender, message.text, newTemplate)
        if (newStatus !is Message.Matched) {
            eventLogger.log(MessageTemplateFailed(Status.Evaluation))
            return null
        }

        messageTemplateService.repository.save(newTemplate)

        message.status = newStatus
        repository.save(message)

        coroutineService.backgroundTask {
            promoteSiblings(message, newTemplate)
        }

        return message
    }

    private suspend fun promoteSiblings(message: Message, template: MessageTemplate) {
        val siblings = repository.getBySender(message.sender)
            .filter { it.status is Message.Recommended }

        siblings.forEach { sibling ->
            val status = matchesTemplate(sibling.sender, sibling.text, template)
            if (status is Message.Matched) {
                sibling.status = status
                repository.save(sibling)
            }
        }
    }

    private fun matchesTemplate(sender: String, text: String, template: MessageTemplate): Message.Status? {
        if (!sender.equals(template.sender, true)) {
            return null
        }

        val result = template.parse(text) ?: return null

        return if (template.isEnabled) {
            Message.Matched(
                messageTemplateId = template.id,
                amount = result.amount,
                merchant = result.merchant
            )
        } else {
            Message.Rejected
        }
    }

    private suspend fun createSpending(message: Message) {
        val status = message.status as? Message.Matched ?: return
        val merchant = status.merchant ?: return
        val category = categoryService.findCategoryByMerchant(merchant) ?: return
        val currency = messageTemplateService.repository.getById(status.messageTemplateId)?.currency ?: return

        message.spendingId = budgetService.spend(
            date = message.receivedAt,
            amount = status.amount,
            expenditureName = category.name,
            comment = message.text,
            currency = currency
        )
    }

    private fun assessStatus(sender: String, text: String): Message.Status =
        isMatched(sender, text) ?: isRecommended(sender, text) ?: Message.Rejected

    private fun isMatched(sender: String, text: String): Message.Status? =
        messageTemplateService.repository.getAll()
            .asSequence()
            .map { matchesTemplate(sender, text, it) }
            .filterNotNull()
            .firstOrNull()

    private fun isRecommended(sender: String, text: String): Message.Status? =
        if (qualifierService.isRecommended(sender, text) && settings.autodetect) {
            Message.Recommended
        } else {
            null
        }

    private fun logEvent(status: Message.Status) {
        val eventStatus = when (status) {
            is Message.Matched -> if (status.merchant != null) {
                MessageEvaluated.Status.Spent
            } else {
                MessageEvaluated.Status.Pending
            }

            is Message.Recommended -> MessageEvaluated.Status.Recommended
            is Message.Rejected -> MessageEvaluated.Status.Rejected
        }
        eventLogger.log(MessageEvaluated(eventStatus))
    }
}
