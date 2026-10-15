package com.mas.mobile.testing

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.mas.mobile.R

/**
 * Every test launches into a fresh, unseeded [TestPersistenceModule] database, so
 * `SettingsService.needToShowPolicy()` (comparing the empty default `policyVersion` against
 * `SettingsService.POLICY_VERSION`) is always true on first launch, and `MainActivity` starts
 * `PolicyActivity` on top of itself before the rest of the app is usable.
 *
 * Call this right after `ActivityScenario.launch(MainActivity::class.java)`, before interacting
 * with anything else, to get past it - mirrors the real first-run user flow rather than seeding
 * around it.
 */
fun agreeToPolicy() {
    onView(withId(R.id.btn_policy_agree)).perform(click())
}
