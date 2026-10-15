package com.mas.mobile.service

import android.content.Context
import com.mas.mobile.R
import com.mas.mobile.domain.budget.Recurrence
import com.mas.mobile.util.DateTool
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceService @Inject constructor(
    private val context: Context
) {
    fun budgetTruncatedComment(date: LocalDate): String {
        val mask = context.getString(R.string.constant_budget_truncated)
        return String.format(mask, DateTool.dateToString(date))
    }

    fun btnSpendingParseMessage() = context.getString(R.string.btn_spending_parse_message)

    fun budgetChangeDateMessage() = context.getString(R.string.message_budget_date)
    fun budgetRemoveMessage() = context.getString(R.string.message_budget_removing)

    fun messageLinkMerchant(vararg args: Any) =
        String.format(context.getString(R.string.message_link_merchant), *args)

    fun messageSpendingNotFound() = context.getString(R.string.message_spending_not_found)

    fun messageAreYouSure() = context.getString(R.string.dialog_confirmation_remove)
    fun messageAllowNotifications() = context.getString(R.string.message_allow_notifications)
    fun messageSettingsStartDay() = context.getString(R.string.message_settings_start_day)
    fun messageSettingsPeriod() = context.getString(R.string.message_settings_period)
    fun messageCapturingDisabled() = context.getString(R.string.message_capturing_disabled)
    fun messageAnalyticsNoAlerts() = context.getString(R.string.message_analytics_no_alerts)
    fun messageAnalyticsNoBudgets() = context.getString(R.string.message_analytics_no_budgets)

    fun messageBackupExportDone() = context.getString(R.string.message_backup_export_done)
    fun messageBackupImportDone(imported: Int, skipped: Int) =
        String.format(context.getString(R.string.message_backup_import_done), imported, skipped)
    fun messageBackupFailed() = context.getString(R.string.message_backup_failed)

    fun constantToday() = context.getString(R.string.constant_today)
    fun constantYesterday() = context.getString(R.string.constant_yesterday)

    fun constantPeriodWeek() = context.getString(R.string.constant_period_week)
    fun constantPeriodTwoWeeks() = context.getString(R.string.constant_period_two_weeks)
    fun constantPeriodMonths() = context.getString(R.string.constant_period_month)
    fun constantPeriodQuarter() = context.getString(R.string.constant_period_quarter)
    fun constantPeriodYear() = context.getString(R.string.constant_period_year)
    fun constantRecurrenceMap() =
        mapOf(
            Recurrence.Never to context.getString(R.string.constant_recurrence_never),
            Recurrence.Daily to context.getString(R.string.constant_recurrence_daily),
            Recurrence.Weekly to context.getString(R.string.constant_recurrence_weekly),
            Recurrence.Monthly to context.getString(R.string.constant_recurrence_monthly),
            Recurrence.Quarterly to context.getString(R.string.constant_recurrence_quarterly)
        )

    fun dialogConfirmationOk() = context.getString(R.string.dialog_confirmation_ok)
    fun dialogConfirmationCancel() = context.getString(R.string.dialog_confirmation_cancel)
    fun dialogCurrencyTitle() = context.getString(R.string.dialog_currency_title)
    fun dialogBlacklist() = context.getString(R.string.dialog_blacklist)

    fun tabQualifierCatch() = context.getString(R.string.tab_qualifier_catch)
    fun tabQualifierSkip() = context.getString(R.string.tab_qualifier_skip)
    fun tabQualifierBlacklist() = context.getString(R.string.tab_qualifier_blacklist)

    fun notificationNewVersionTitle() = context.getString(R.string.notification_new_version_title)
    fun notificationNewVersionText() = context.getString(R.string.notification_new_version_text)

    fun labelAnalyticsPlan() = context.getString(R.string.label_analytics_plan)
    fun labelAnalyticsFact() = context.getString(R.string.label_analytics_fact)

    fun hintChartExpenditures() = context.getString(R.string.hint_chart_expenditures)
    fun hintSpendingRecurrences() = context.getString(R.string.hint_spending_recurrences)

    fun validatorPatternFailed() = context.getString(R.string.validator_pattern_failed)

    fun titleScheduled() = context.getString(R.string.title_scheduled)
}
