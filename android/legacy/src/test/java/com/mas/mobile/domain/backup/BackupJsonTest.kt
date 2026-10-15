package com.mas.mobile.domain.backup

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class BackupJsonTest {
    @Test
    fun `round-trips a backup file through JSON including dates`() {
        val backup = BackupFile(
            exportedAt = LocalDateTime.of(2024, 9, 1, 12, 30),
            budgets = listOf(
                BudgetBackup(
                    id = 1,
                    name = "September 2024",
                    plan = 100.0,
                    fact = 25.0,
                    startsOn = LocalDate.of(2024, 9, 1),
                    lastDayAt = LocalDate.of(2024, 9, 30),
                    comment = "truncated",
                    currency = "EUR",
                    expenditures = listOf(
                        ExpenditureBackup(id = 11, name = "Food", iconId = 5, plan = 100.0, fact = 25.0, comment = "", displayOrder = 0)
                    ),
                    spendings = listOf(
                        SpendingBackup(
                            id = 21,
                            comment = "Coffee",
                            date = LocalDateTime.of(2024, 9, 2, 10, 0),
                            amount = 5.0,
                            expenditureId = 11,
                            exchangeCurrency = "USD",
                            exchangeRate = 1.1,
                            exchangeRawAmount = 5.0,
                            recurrence = "Never",
                            messages = listOf(
                                MessageBackup(
                                    id = 31,
                                    sender = "Bank",
                                    text = "Coffee 5.00",
                                    receivedAt = LocalDateTime.of(2024, 9, 2, 9, 59),
                                    isNew = false
                                )
                            )
                        )
                    )
                )
            )
        )

        val roundTripped = BackupJson.fromJson(BackupJson.toJson(backup))

        assertEquals(backup, roundTripped)
    }
}
