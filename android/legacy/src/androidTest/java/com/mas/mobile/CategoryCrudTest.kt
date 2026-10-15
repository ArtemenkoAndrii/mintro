package com.mas.mobile

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
 * Covers the full category lifecycle through the real UI: add, view (fields disabled per
 * `CategoryFragment`'s `setUpEditable()`), edit, and delete - asserting at each step that name,
 * plan, and merchant round-trip exactly what was entered (`CategoryViewModel.kt`).
 */
@RunWith(AndroidJUnit4::class)
class CategoryCrudTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    @Test
    fun addViewEditAndDeleteACategory() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        TestFixtures.seedActiveBudget(db)

        ActivityScenario.launch(MainActivity::class.java).use {
            agreeToPolicy()

            onView(withId(R.id.nav_menu)).perform(click())
            onView(withId(R.id.menuCategoriesLayout)).perform(click())

            // Add
            onView(withId(R.id.category_add_btn)).perform(click())
            onView(withId(R.id.categoryName)).perform(replaceText("Streaming"))
            onView(withId(R.id.spendingAmount)).perform(replaceText("50.00"), closeSoftKeyboard())
            onView(withId(R.id.addChipEditText)).perform(replaceText("Netflix"))
            onView(withId(R.id.addChipButton)).perform(click())
            onView(withId(R.id.merchantChipGroup)).check(matches(hasDescendant(withText("Netflix"))))
            onView(withId(R.id.categorySaveButton)).perform(click())

            onView(withId(R.id.category_list)).check(
                matches(hasDescendant(allOf(withId(R.id.categoryRowName), withText("Streaming"))))
            )
            onView(withId(R.id.category_list)).check(
                matches(hasDescendant(allOf(withId(R.id.categoryRowPlan), withText("50.00"))))
            )

            // View round-trip: fields show exactly what was entered, and are not editable.
            onView(withId(R.id.category_list)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Streaming")), click()
                )
            )
            onView(withId(R.id.categoryName)).check(matches(withText("Streaming")))
            onView(withId(R.id.merchantChipGroup)).check(matches(hasDescendant(withText("Netflix"))))
            onView(withId(R.id.categorySaveButton)).check(matches(not(isDisplayed())))
            pressBack()

            // Edit
            onView(withId(R.id.category_list)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Streaming")), clickChildView(R.id.categoryRowMenu)
                )
            )
            onView(withText(R.string.menu_edit)).perform(click())
            onView(withId(R.id.categoryName)).check(matches(withText("Streaming")))
            onView(withId(R.id.spendingAmount)).perform(replaceText("75.00"), closeSoftKeyboard())
            onView(withId(R.id.categorySaveButton)).perform(click())

            onView(withId(R.id.category_list)).check(
                matches(hasDescendant(allOf(withId(R.id.categoryRowPlan), withText("75.00"))))
            )

            // Delete
            onView(withId(R.id.category_list)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Streaming")), clickChildView(R.id.categoryRowMenu)
                )
            )
            onView(withText(R.string.menu_remove)).perform(click())
            onView(withText(R.string.dialog_confirmation_yes)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.category_list)).check(
                matches(not(hasDescendant(withText("Streaming"))))
            )
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
