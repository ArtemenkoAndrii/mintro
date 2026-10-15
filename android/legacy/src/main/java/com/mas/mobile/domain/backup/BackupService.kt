package com.mas.mobile.domain.backup

import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.BudgetRepository
import com.mas.mobile.domain.budget.ExchangeInfo
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.domain.budget.ExpenditureId
import com.mas.mobile.domain.budget.ExpenditureRepository
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.domain.budget.Recurrence
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.domain.budget.SpendingRepository
import com.mas.mobile.domain.message.Message
import com.mas.mobile.domain.message.MessageRepository
import java.time.LocalDateTime
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenditureRepository: ExpenditureRepository,
    private val spendingRepository: SpendingRepository,
    private val messageRepository: MessageRepository
) {
    fun buildExport(): BackupFile =
        BackupFile(
            exportedAt = LocalDateTime.now(),
            budgets = budgetRepository.getAll().map { it.toBackup() }
        )

    suspend fun importBudgets(backup: BackupFile): ImportSummary {
        var imported = 0
        var skipped = 0

        backup.budgets.forEach { budgetBackup ->
            if (budgetRepository.getBudgetByName(budgetBackup.name) != null) {
                skipped++
            } else {
                importBudget(budgetBackup)
                imported++
            }
        }

        return ImportSummary(imported, skipped)
    }

    private suspend fun importBudget(budgetBackup: BudgetBackup) {
        val budget = budgetRepository.createBudget().also {
            it.name = budgetBackup.name
            it.startsOn = budgetBackup.startsOn
            it.lastDayAt = budgetBackup.lastDayAt
            it.comment = budgetBackup.comment
            it.currency = Currency.getInstance(budgetBackup.currency)
        }

        val expenditureIdMap = mutableMapOf<Int, ExpenditureId>()
        budgetBackup.expenditures.forEach { expenditureBackup ->
            val expenditure = expenditureRepository.create().also {
                it.name = expenditureBackup.name
                it.iconId = expenditureBackup.iconId?.let { icon -> IconId(icon) }
                it.plan = expenditureBackup.plan
                it.fact = expenditureBackup.fact
                it.comment = expenditureBackup.comment
                it.displayOrder = expenditureBackup.displayOrder
            }
            budget.addExpenditure(expenditure)
            expenditureIdMap[expenditureBackup.id] = expenditure.id
        }

        budgetBackup.spendings.forEach { spendingBackup ->
            val expenditureId = expenditureIdMap[spendingBackup.expenditureId]
                ?: return@forEach
            val expenditure = budget.getExpenditure(expenditureId) ?: return@forEach

            val spending = spendingRepository.create().also {
                it.comment = spendingBackup.comment
                it.date = spendingBackup.date
                it.amount = spendingBackup.amount
                it.expenditure = expenditure
                it.recurrence = try {
                    Recurrence.valueOf(spendingBackup.recurrence)
                } catch (e: IllegalArgumentException) {
                    Recurrence.Never
                }
                it.exchangeInfo = spendingBackup.exchangeCurrency?.let { currencyCode ->
                    ExchangeInfo(
                        rawAmount = spendingBackup.exchangeRawAmount ?: 0.0,
                        rate = spendingBackup.exchangeRate ?: 0.0,
                        currency = Currency.getInstance(currencyCode)
                    )
                }
            }
            budget.addSpending(spending)

            spendingBackup.messages.forEach { messageBackup ->
                importMessage(messageBackup, spending)
            }
        }

        budgetRepository.save(budget)
    }

    private suspend fun importMessage(messageBackup: MessageBackup, spending: Spending) {
        val message = messageRepository.create().also {
            it.sender = messageBackup.sender
            it.text = messageBackup.text
            it.receivedAt = messageBackup.receivedAt
            it.status = Message.Recommended
            it.isNew = false
            it.spendingId = spending.id
        }
        messageRepository.save(message)
    }

    private fun Budget.toBackup() = BudgetBackup(
        id = id.value,
        name = name,
        plan = plan,
        fact = fact,
        startsOn = startsOn,
        lastDayAt = lastDayAt,
        comment = comment,
        currency = currency.currencyCode,
        expenditures = budgetDetails.expenditure.map { it.toBackup() },
        spendings = budgetDetails.spending.map { it.toBackup() }
    )

    private fun Expenditure.toBackup() = ExpenditureBackup(
        id = id.value,
        name = name,
        iconId = iconId?.value,
        plan = plan,
        fact = fact,
        comment = comment,
        displayOrder = displayOrder
    )

    private fun Spending.toBackup() = SpendingBackup(
        id = id.value,
        comment = comment,
        date = date,
        amount = amount,
        expenditureId = expenditure.id.value,
        exchangeCurrency = exchangeInfo?.currency?.currencyCode,
        exchangeRate = exchangeInfo?.rate,
        exchangeRawAmount = exchangeInfo?.rawAmount,
        recurrence = recurrence.name,
        messages = messageRepository.getBySpendingId(id)?.let { listOf(it.toBackup()) } ?: emptyList()
    )

    private fun Message.toBackup() = MessageBackup(
        id = id.value,
        sender = sender,
        text = text,
        receivedAt = receivedAt,
        isNew = isNew
    )
}
