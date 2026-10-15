package com.mas.mobile

import android.view.View
import android.widget.NumberPicker
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.testing.FakeAppComponentRule
import com.mas.mobile.testing.TestFixtures
import com.mas.mobile.testing.agreeToPolicy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Exercises `BudgetService`'s period-creation math (`BudgetService.kt`: `createBudget()` /
 * `prepareXBudget()` / `generateComment()`) through the real Settings -> Budgets UI, once per
 * `Period` type.
 *
 * The app always starts on `ExpenditureListFragment`, and `BudgetService`'s `init` block calls
 * `getActiveBudget()` immediately, so a budget already exists by the time the activity is up -
 * before a test can touch Settings. To keep that auto-created budget out of the way, a placeholder
 * budget that closes exactly *today* is seeded directly (bypassing `createBudget()`); this makes
 * `getActiveBudget()` find it and skip auto-creation, so the very first budget this test creates
 * for real starts "tomorrow" - a date it fully controls.
 *
 * For MONTH/WEEK/TWO_WEEKS, a start-day setting deliberately different from tomorrow's actual
 * day is chosen through the real Settings UI, which guarantees the first created budget is
 * truncated (`estimatedStartDate < actualStartDate`). The second budget, created right after via
 * "Add budget", is mathematically guaranteed to never be truncated: `createNext()`'s start date is
 * always the prior budget's `lastDayAt + 1`, which by construction of `prepareXBudget()` is always
 * exactly the configured period's own start day. QUARTER/YEAR have no start-day setting (pure
 * calendar math), so their "truncated" expectation is computed the same way `BudgetService` does
 * rather than assumed - correct (never flaky) on every possible run date.
 */
@RunWith(AndroidJUnit4::class)
class BudgetCreationTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @Test
    fun weeklyBudgetsAreCreatedWithCorrectNameDateRangeAndTruncation() {
        val mismatchDay = mismatchDayOfWeek()
        verifyPeriod(
            periodLabel = R.string.constant_period_week,
            configureStartDay = { selectStartDayOfWeek(mismatchDay) },
            computeBudget = { actualStartDate -> computeWeekBudget(actualStartDate, mismatchDay, extraDays = 0) }
        )
    }

    @Test
    fun twoWeeksBudgetsAreCreatedWithCorrectNameDateRangeAndTruncation() {
        val mismatchDay = mismatchDayOfWeek()
        verifyPeriod(
            periodLabel = R.string.constant_period_two_weeks,
            configureStartDay = { selectStartDayOfWeek(mismatchDay) },
            computeBudget = { actualStartDate -> computeWeekBudget(actualStartDate, mismatchDay, extraDays = 7) }
        )
    }

    @Test
    fun monthlyBudgetsAreCreatedWithCorrectNameDateRangeAndTruncation() {
        val mismatchDay = mismatchDayOfMonth()
        verifyPeriod(
            periodLabel = R.string.constant_period_month,
            configureStartDay = { selectStartDayOfMonth(mismatchDay) },
            computeBudget = { actualStartDate -> computeMonthBudget(actualStartDate, mismatchDay) }
        )
    }

    @Test
    fun quarterlyBudgetsAreCreatedWithCorrectNameDateRangeAndTruncation() {
        verifyPeriod(
            periodLabel = R.string.constant_period_quarter,
            configureStartDay = {},
            computeBudget = { actualStartDate -> computeQuarterBudget(actualStartDate) }
        )
    }

    @Test
    fun yearlyBudgetsAreCreatedWithCorrectNameDateRangeAndTruncation() {
        verifyPeriod(
            periodLabel = R.string.constant_period_year,
            configureStartDay = {},
            computeBudget = { actualStartDate -> computeYearBudget(actualStartDate) }
        )
    }

    /**
     * `computeBudget` mirrors the matching `prepareXBudget()` in `BudgetService.kt` and returns
     * (estimatedStartDate, estimatedEndDate, name) for a given real/actual start date.
     */
    private fun verifyPeriod(
        periodLabel: Int,
        configureStartDay: () -> Unit,
        computeBudget: (LocalDate) -> Triple<LocalDate, LocalDate, String>
    ) {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        TestFixtures.seedBudget(
            db,
            name = "Placeholder",
            startsOn = LocalDate.now().minusDays(30),
            lastDayAt = LocalDate.now()
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            agreeToPolicy()

            openSettings()
            selectPeriod(periodLabel)
            configureStartDay()

            openBudgetList()

            val actualStartA = LocalDate.now().plusDays(1)
            val (estimatedStartA, endA, nameA) = computeBudget(actualStartA)
            onView(withId(R.id.budget_add_btn)).perform(click())
            assertBudgetRow(app, name = nameA, startsOn = actualStartA, lastDayAt = endA, estimatedStart = estimatedStartA)

            val actualStartB = endA.plusDays(1)
            val (estimatedStartB, endB, nameB) = computeBudget(actualStartB)
            onView(withId(R.id.budget_add_btn)).perform(click())
            assertBudgetRow(app, name = nameB, startsOn = actualStartB, lastDayAt = endB, estimatedStart = estimatedStartB)
        }
    }

    private fun openSettings() {
        onView(withId(R.id.nav_menu)).perform(click())
        onView(withId(R.id.menuSettingsLayout)).perform(click())
    }

    private fun openBudgetList() {
        // The bottom nav bar (including nav_menu) is only shown on root destinations; Settings is
        // a child destination pushed onto the "Menu" tab's own back stack, so nav_menu isn't on
        // screen to re-click here - pop back to the tab's root (MenuFragment) instead.
        pressBack()
        onView(withId(R.id.menuBudgetsLayout)).perform(click())
    }

    private fun selectPeriod(labelRes: Int) {
        onView(withId(R.id.settingsPeriodLayout)).perform(click())
        onView(withText(labelRes)).inRoot(isDialog()).perform(click())
        onView(withText(R.string.dialog_confirmation_ok)).inRoot(isDialog()).perform(click())
    }

    private fun selectStartDayOfMonth(day: Int) {
        onView(withId(R.id.settingsStartDayOfMonth)).perform(click())
        onView(isAssignableFrom(NumberPicker::class.java)).inRoot(isDialog())
            .perform(setNumberPickerValue(day - 1))
        onView(withText(R.string.dialog_confirmation_ok)).inRoot(isDialog()).perform(click())
    }

    private fun selectStartDayOfWeek(day: DayOfWeek) {
        onView(withId(R.id.settingsStartDayOfWeek)).perform(click())
        onView(isAssignableFrom(NumberPicker::class.java)).inRoot(isDialog())
            .perform(setNumberPickerValue(day.ordinal))
        onView(withText(R.string.dialog_confirmation_ok)).inRoot(isDialog()).perform(click())
    }

    /**
     * `espresso-contrib:3.6.1` dropped `PickerActions.setNumberPickerValue` (only `setDate`/
     * `setTime` remain), and `SettingsFragment.showListPicker()`'s `NumberPicker` has no
     * `android:id` to target with a plain `ViewActions` call - so set its value directly.
     */
    private fun setNumberPickerValue(value: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(NumberPicker::class.java)
        override fun getDescription() = "Set NumberPicker value to $value"
        override fun perform(uiController: UiController, view: View) {
            (view as NumberPicker).value = value
            uiController.loopMainThreadUntilIdle()
        }
    }

    /**
     * Opens the just-created budget's row (matched by its generated name) and asserts the detail
     * screen's stored dates, plus the truncation comment - present only when `estimatedStart` is
     * strictly before the real `startsOn`, exactly mirroring `BudgetService.generateComment()`.
     */
    private fun assertBudgetRow(
        app: MasApplication,
        name: String,
        startsOn: LocalDate,
        lastDayAt: LocalDate,
        estimatedStart: LocalDate
    ) {
        onView(withId(R.id.budget_list)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(name)), click()
            )
        )

        onView(withId(R.id.budgetStartsOn)).check(matches(withText(startsOn.format(dateFormat))))
        onView(withId(R.id.budgetLastDayAt)).check(matches(withText(lastDayAt.format(dateFormat))))

        val expectedComment = if (estimatedStart < startsOn) {
            String.format(app.getString(R.string.constant_budget_truncated), estimatedStart.format(dateFormat))
        } else {
            ""
        }
        onView(withHint(R.string.hint_budget_comment)).check(matches(withText(expectedComment)))

        pressBack()
    }

    private fun mismatchDayOfWeek(): DayOfWeek {
        val tomorrow = LocalDate.now().plusDays(1).dayOfWeek
        return if (tomorrow == DayOfWeek.MONDAY) DayOfWeek.TUESDAY else DayOfWeek.MONDAY
    }

    private fun mismatchDayOfMonth(): Int {
        val tomorrow = LocalDate.now().plusDays(1).dayOfMonth
        return if (tomorrow == 1) 15 else 1
    }

    private fun computeWeekBudget(
        actualStartDate: LocalDate,
        periodStartDay: DayOfWeek,
        extraDays: Long
    ): Triple<LocalDate, LocalDate, String> {
        val diff = if (actualStartDate.dayOfWeek.value >= periodStartDay.value) {
            actualStartDate.dayOfWeek.value - periodStartDay.value
        } else {
            7 - periodStartDay.value + actualStartDate.dayOfWeek.value
        }
        val estimatedStart = actualStartDate.minusDays(diff.toLong())
        val estimatedEnd = estimatedStart.plusDays(6 + extraDays)
        val name = estimatedStart.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy"))
        return Triple(estimatedStart, estimatedEnd, name)
    }

    private fun computeMonthBudget(actualStartDate: LocalDate, periodStartDay: Int): Triple<LocalDate, LocalDate, String> {
        val estimatedStart = if (actualStartDate.dayOfMonth >= periodStartDay) {
            actualStartDate.withDayOfMonth(periodStartDay)
        } else {
            actualStartDate.minusMonths(1).withDayOfMonth(periodStartDay)
        }
        val estimatedEnd = estimatedStart.plusMonths(1).minusDays(1)
        val name = estimatedStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        return Triple(estimatedStart, estimatedEnd, name)
    }

    private fun computeQuarterBudget(actualStartDate: LocalDate): Triple<LocalDate, LocalDate, String> {
        val quarterlyMonth = listOf(10, 7, 4, 1).first { actualStartDate.month.value >= it }
        val estimatedStart = actualStartDate.withMonth(quarterlyMonth).withDayOfMonth(1)
        val estimatedEnd = estimatedStart.plusMonths(2).let { it.withDayOfMonth(it.month.length(it.isLeapYear)) }
        val name = estimatedStart.format(DateTimeFormatter.ofPattern("QQQ yyyy"))
        return Triple(estimatedStart, estimatedEnd, name)
    }

    private fun computeYearBudget(actualStartDate: LocalDate): Triple<LocalDate, LocalDate, String> {
        val estimatedStart = actualStartDate.withMonth(1).withDayOfMonth(1)
        val estimatedEnd = estimatedStart.withMonth(12).let { it.withDayOfMonth(it.month.length(it.isLeapYear)) }
        val name = estimatedStart.format(DateTimeFormatter.ofPattern("yyyy"))
        return Triple(estimatedStart, estimatedEnd, name)
    }
}
