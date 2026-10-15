package com.mas.mobile.domain.backup

import java.time.LocalDate
import java.time.LocalDateTime

data class BackupFile(
    val schemaVersion: Int = 1,
    val exportedAt: LocalDateTime,
    val budgets: List<BudgetBackup>
)

data class BudgetBackup(
    val id: Int,
    val name: String,
    val plan: Double,
    val fact: Double,
    val startsOn: LocalDate,
    val lastDayAt: LocalDate,
    val comment: String?,
    val currency: String,
    val expenditures: List<ExpenditureBackup>,
    val spendings: List<SpendingBackup>
)

data class ExpenditureBackup(
    val id: Int,
    val name: String,
    val iconId: Int?,
    val plan: Double,
    val fact: Double,
    val comment: String,
    val displayOrder: Int
)

data class SpendingBackup(
    val id: Int,
    val comment: String,
    val date: LocalDateTime,
    val amount: Double,
    val expenditureId: Int,
    val exchangeCurrency: String?,
    val exchangeRate: Double?,
    val exchangeRawAmount: Double?,
    val recurrence: String,
    val messages: List<MessageBackup>
)

data class MessageBackup(
    val id: Int,
    val sender: String,
    val text: String,
    val receivedAt: LocalDateTime,
    val isNew: Boolean
)

data class ImportSummary(val imported: Int, val skipped: Int)
