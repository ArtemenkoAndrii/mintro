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
import com.mas.mobile.repository.db.entity.Qualifier
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
 * Covers the message-matching pipeline for messages sourced from `NotificationListener` rather
 * than `SmsListener` (see `SmsMessageCaptureTest`, whose "matched but unmapped merchant" scenario
 * this mirrors). `NotificationListener.onNotificationPosted` builds its raw text as
 * `"$title\n$text"` (NotificationListener.kt:33) before calling the same
 * `MessageService.handleRawMessage` used by SMS, so this exercises `Pattern.escapeNewLines()`
 * collapsing that embedded newline before matching - the behavior fixed by
 * commit d598e93 ("fix: multiline patterns").
 */
@RunWith(AndroidJUnit4::class)
class NotificationMessageCaptureTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    @Test
    fun matchedNotificationWithMappedMerchantCreatesASpendingWhileUnmappedOneDoesNot() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        TestFixtures.seedActiveBudget(db)
        TestFixtures.seedCategory(db, id = 1, name = "Restaurants", merchants = listOf("McDonalds"))
        TestFixtures.seedMessageTemplate(
            db,
            sender = "Revolut",
            pattern = "Paid \${amount} at {merchant} Spent",
            example = "Revolut\nPaid \$55.70 at McDonalds Spent today: \$55.70"
        )

        // "$title\n$text" is exactly the shape NotificationListener.onNotificationPosted builds
        // from a StatusBarNotification's extracted title/text before calling handleRawMessage.
        val mappedMerchantText = "Revolut\nPaid \$55.70 at McDonalds Spent today: \$55.70"
        val unmappedMerchantText = "Revolut\nPaid \$32.10 at Costco Spent today: \$32.10"

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
            // capturing on first visit - dismiss it to stay on the message list.
            onView(withText(R.string.dialog_confirmation_no)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.message_list)).check(matches(hasDescendant(withText(mappedMerchantText))))
            onView(withId(R.id.message_list)).check(matches(hasDescendant(withText(unmappedMerchantText))))
            assertRecyclerViewItemCount(R.id.message_list, 2)

            // Revolut/McDonalds message: merchant mapped to "Restaurants" -> spent/linked look.
            onView(withId(R.id.message_list)).check(
                matches(hasDescendant(allOf(
                    hasDescendant(withText(mappedMerchantText)),
                    hasDescendant(allOf(withId(R.id.messageListRowOkIcon), isDisplayed()))
                )))
            )
            // Revolut/Costco message: matched template, but no category has "Costco" as a
            // merchant -> pending look (question icon), not the spent one.
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

    @Test
    fun unmatchedNotificationFromAnUnconfiguredAppBecomesRecommendedWithoutCreatingASpending() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()
        TestFixtures.seedActiveBudget(db)
        // No MessageTemplate is seeded, so nothing can ever reach Message.Matched here - this
        // is the common notification-capture case: an app the user hasn't configured a pattern
        // for yet. QualifierService.isRecommended still needs at least one CATCH keyword seeded,
        // since the shipped DML.kt qualifiers aren't loaded into the empty in-memory test DB.
        TestFixtures.seedQualifier(db, type = Qualifier.CATCH, name = "paid")

        // Contains a CATCH keyword ("paid"), an amount+currency ("$45.00"), no SKIP keyword, and
        // "PayApp" isn't blacklisted - satisfies QualifierService.isRecommended.
        val recommendedText = "Payment Alert\nYou paid \$45.00 for groceries"

        app.appComponent.messageService().handleRawMessage(
            sender = "PayApp",
            text = recommendedText,
            date = LocalDateTime.now()
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            agreeToPolicy()

            onView(withId(R.id.nav_message_list_fragment)).perform(click())
            onView(withText(R.string.dialog_confirmation_no)).inRoot(isDialog()).perform(click())

            onView(withId(R.id.message_list)).check(matches(hasDescendant(withText(recommendedText))))
            assertRecyclerViewItemCount(R.id.message_list, 1)

            // Message.Recommended rendering (SpendingMessageAdapter.kt:77-79): info icon plus the
            // "recommended" layout (offering to promote it into a template), not the matched one.
            onView(withId(R.id.message_list)).check(
                matches(hasDescendant(allOf(
                    hasDescendant(withText(recommendedText)),
                    hasDescendant(allOf(withId(R.id.messageListRowInfoIcon), isDisplayed())),
                    hasDescendant(allOf(withId(R.id.messageListRecommendedLayout), isDisplayed()))
                )))
            )

            onView(withId(R.id.nav_spending_list)).perform(click())
            assertRecyclerViewItemCount(R.id.spendingList, 0)
        }
    }

    private fun assertRecyclerViewItemCount(id: Int, expectedCount: Int) {
        onView(withId(id)).check { view, _ ->
            val actualCount = (view as RecyclerView).adapter?.itemCount ?: 0
            assertEquals(expectedCount, actualCount)
        }
    }
}
