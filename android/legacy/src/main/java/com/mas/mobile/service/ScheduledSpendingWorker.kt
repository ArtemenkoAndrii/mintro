package com.mas.mobile.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mas.mobile.appComponent
import com.mas.mobile.domain.budget.BudgetService
import javax.inject.Inject

class ScheduledSpendingWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    @Inject
    lateinit var budgetService: BudgetService

    override fun doWork(): Result {
        budgetService.createScheduledSpendings()
        return Result.success()
    }

    init {
        context.appComponent.injectScheduledSpendingWorker(this)
    }
}