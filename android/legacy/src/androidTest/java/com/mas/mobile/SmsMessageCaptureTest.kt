package com.mas.mobile

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Extends the message-matching pipeline coverage (see the now-removed
 * `MessageToSpendingPipelineTest`, which this reuses the same Revolut sender/pattern/example
 * from `DML.kt` as) with the "matched but unmapped merchant" branch
 * (`MessageService.kt`/`SpendingMessageAdapter.kt:56-89`): a message can match a template's
 * pattern and still not turn into a spending if its merchant isn't linked to any `Category`.
 */
@RunWith(AndroidJUnit4::class)
class SmsMessageCaptureTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    @Test
    fun matchedMessageWithMappedMerchantCreatesASpendingWhileUnmappedOneDoesNot() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        TestFixtures.seedActiveBudget(db)
        TestFixtures.seedCategory(db, id = 1, name = "Restaurants", merchants = listOf("McDonalds"))
        TestFixtures.seedMessageTemplate(
            db,
            sender = "Revolut",
            pattern = "Paid \${amount} at {merchant} Spent",
            example = "McDonalds 🛍 Paid \$55.70 at McDonalds Spent today: \$55.70"
        )

        val mappedMerchantText = "McDonalds 🛍 Paid \$55.70 at McDonalds Spent today: \$55.70"
        val unmappedMerchantText = "Costco 🛍 Paid \$32.10 at Costco Spent today: \$32.10"

        // SynchronousTaskService makes handleRawMessage resolve synchronously, so both messages
        // are fully processed (and, for the first, its spending already exists) before launch.
        app.appComponent.messageService().handleRawMessage(
            sender = "Revolut",
            text = mappedMerchantText,
            date = LocalDateTime.now()
        )
        app.appComponent.messageService().handleRawMessage(
            sender = "Revolut",
            text = unmappedMerchantText,
            date = LocalDateTime.now()
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            agreeToPolicy()

            onView(withId(R.id.nav_message_list_fragment)).perform(click())

            // Neither SMS nor notification capture is enabled (both default off, and this test
            // never turns them on), so MessageListFragment.showDialog() always prompts to enable
            // capturing on first visit (scenario 6.5) - dismiss it to stay on the message list.
            onView(withText(R.string.dialog_confirmation_no)).inRoot(isDialog()).perform(click())

            // Both captured messages are actually displayed in the list, by their exact text
            // (message_list_row.xml's message_list_row_message, bound to `message.text`) - this
            // is the direct proof that handleRawMessage() results in a visible list entry, not
            // just an icon-state side effect.
            onView(withId(R.id.message_list)).check(matches(hasDescendant(withText(mappedMerchantText))))
            onView(withId(R.id.message_list)).check(matches(hasDescendant(withText(unmappedMerchantText))))
            assertRecyclerViewItemCount(R.id.message_list, 2)

            // McDonalds message: merchant mapped to "Restaurants" -> spent/linked look, on the
            // same row as its text (not just present anywhere in the list).
            onView(withId(R.id.message_list)).check(
                matches(hasDescendant(allOf(
                    hasDescendant(withText(mappedMerchantText)),
                    hasDescendant(allOf(withId(R.id.messageListRowOkIcon), isDisplayed()))
                )))
            )
            // Costco message: matched template, but no category has "Costco" as a merchant ->
            // pending look (question icon + "link to category" button), not the spent one.
            onView(withId(R.id.message_list)).check(
                matches(hasDescendant(allOf(
                    hasDescendant(withText(unmappedMerchantText)),
                    hasDescendant(allOf(withId(R.id.messageListRowQuestionIcon), isDisplayed())),
                    hasDescendant(allOf(withId(R.id.messageListMatchedLayout), isDisplayed()))
                )))
            )

            onView(withId(R.id.nav_spending_list)).perform(click())
            onView(withId(R.id.spendingList)).check(matches(hasDescendant(withText("Restaurants"))))
            assertRecyclerViewItemCount(R.id.spendingList, 1)
        }
    }

    private fun assertRecyclerViewItemCount(id: Int, expectedCount: Int) {
        onView(withId(id)).check { view, _ ->
            val actualCount = (view as RecyclerView).adapter?.itemCount ?: 0
            assertEquals(expectedCount, actualCount)
        }
    }
}
