package com.mas.mobile

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.testing.FakeAppComponentRule
import com.mas.mobile.testing.TestFixtures
import com.mas.mobile.testing.agreeToPolicy
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers add/edit/delete of a spending through the real UI, asserting that each change is
 * reflected in three places at once: `spendingList`, the `expenditureList` row's fact
 * (`Budget.calculate()`), and the overall budget progress text (`Budget.getStatus()`, which
 * `Budget.getProgress()` - the `%` shown on `expenditureList`'s progress bar - is derived from).
 */
@RunWith(AndroidJUnit4::class)
class SpendingCrudTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    @Test
    fun addEditAndDeleteASpendingUpdatesListsAndProgress() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        val budgetId = TestFixtures.seedActiveBudget(db)
        TestFixtures.seedExpenditure(db, budgetId, name = "Groceries", plan = 50.0)

        ActivityScenario.launch(MainActivity::class.java).use {
            agreeToPolicy()

            // Starts on expenditureList (nav start destination).
            onView(withId(R.id.expenditureList)).check(matches(hasDescendant(withText("Groceries"))))
            onView(withId(R.id.expenditureListStatus)).check(matches(withText("0.00 / 50.00")))

            // Add
            onView(withId(R.id.expenditureList)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Groceries")), click()
                )
            )
            onView(withId(R.id.spendingAmount)).perform(replaceText("25"), closeSoftKeyboard())
            onView(withId(R.id.spendingSaveButton)).perform(click())

            onView(withId(R.id.expenditureList)).check(
                matches(hasDescendant(allOf(withId(R.id.expenditure_row_fact), withText("25.00"))))
            )
            onView(withId(R.id.expenditureListStatus)).check(matches(withText("25.00 / 50.00")))

            onView(withId(R.id.nav_spending_list)).perform(click())
            onView(withId(R.id.spendingList)).check(
                matches(hasDescendant(allOf(withId(R.id.spendingListRowAmount), withText("-25.00"))))
            )

            // Edit
            onView(withId(R.id.spendingList)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Groceries")), clickChildView(R.id.spendingRowMenu)
                )
            )
            onView(withText(R.string.menu_edit)).perform(click())
            onView(withId(R.id.spendingAmount)).perform(replaceText("40"), closeSoftKeyboard())
            onView(withId(R.id.spendingSaveButton)).perform(click())

            onView(withId(R.id.spendingList)).check(
                matches(hasDescendant(allOf(withId(R.id.spendingListRowAmount), withText("-40.00"))))
            )

            onView(withId(R.id.nav_expenditure_list)).perform(click())
            onView(withId(R.id.expenditureList)).check(
                matches(hasDescendant(allOf(withId(R.id.expenditure_row_fact), withText("40.00"))))
            )
            onView(withId(R.id.expenditureListStatus)).check(matches(withText("40.00 / 50.00")))

            // Delete
            onView(withId(R.id.nav_spending_list)).perform(click())
            onView(withId(R.id.spendingList)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Groceries")), clickChildView(R.id.spendingRowMenu)
                )
            )
            onView(withText(R.string.menu_remove)).perform(click())
            onView(withText(R.string.dialog_confirmation_yes)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.spendingList)).check(matches(not(hasDescendant(withText("Groceries")))))

            onView(withId(R.id.nav_expenditure_list)).perform(click())
            onView(withId(R.id.expenditureList)).check(
                matches(hasDescendant(allOf(withId(R.id.expenditure_row_fact), withText("0.00"))))
            )
            onView(withId(R.id.expenditureListStatus)).check(matches(withText("0.00 / 50.00")))
        }
    }

    /** Clicks a specific descendant of the RecyclerView item matched by `RecyclerViewActions`. */
    private fun clickChildView(id: Int): ViewAction = object : ViewAction {
        override fun getConstraints() = null
        override fun getDescription() = "Click on a child view with id $id"
        override fun perform(uiController: UiController, view: View) {
            view.findViewById<View>(id).performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }
}
