package com.mas.mobile

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mas.mobile.domain.backup.BackupFile
import com.mas.mobile.domain.backup.BackupJson
import com.mas.mobile.domain.backup.BudgetBackup
import com.mas.mobile.domain.backup.ExpenditureBackup
import com.mas.mobile.domain.backup.SpendingBackup
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.viewmodel.SettingsViewModel
import com.mas.mobile.testing.FakeAppComponentRule
import com.mas.mobile.testing.TestFixtures
import com.mas.mobile.testing.agreeToPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Exercises `BackupService`/`BackupFileService`/`BackupJson` (see `domain/backup/`) end to end
 * through a real, file-backed JSON round trip and the real (in-memory, per-test) database -
 * everything except the OS document-picker chooser itself, which Espresso can't drive without
 * the `espresso-intents` dependency this project doesn't have. The Settings ViewModel's
 * `exportBudgets`/`importBudgets` are therefore invoked directly with a `file://` `Uri`, exactly
 * as `SettingsFragment`'s real `ActivityResultLauncher` callbacks would after a picker result.
 *
 * `SettingsViewModel` (like `ItemViewModel`) depends on the concrete `CoroutineService`, not the
 * `TaskService` interface `TestExternalServicesModule` swaps for a synchronous fake - so its
 * `backgroundTask` calls really do run on a background dispatcher here, same as in production.
 * A scoped `CountingIdlingResource` (same mechanism `TestPersistenceModule` already uses for Room)
 * tracks each export/import call so the `onView()` synchronization point after it genuinely waits
 * for the background work - including the plain file I/O in `BackupFileService`, which no Room
 * idling resource would otherwise cover - before the test reads the file or the database.
 */
@RunWith(AndroidJUnit4::class)
class BackupExportImportTest {
    @get:Rule
    val fakeAppComponentRule = FakeAppComponentRule()

    private val idlingResource = CountingIdlingResource("BackupOperation")

    @Test
    fun exportsBudgetsAndSkipsReimportingAnExistingOne() {
        val app = ApplicationProvider.getApplicationContext<MasApplication>()
        val db = app.appComponent.db()

        val budgetId = TestFixtures.seedActiveBudget(db, name = "Trip", currency = "USD")
        val expenditureId = TestFixtures.seedExpenditure(db, budgetId = budgetId, name = "Food", plan = 100.0)
        TestFixtures.seedSpending(db, expenditureId = expenditureId, amount = 5.0, comment = "Coffee")

        IdlingRegistry.getInstance().register(idlingResource)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                agreeToPolicy()

                onView(withId(R.id.nav_menu)).perform(click())
                onView(withId(R.id.menuSettingsLayout)).perform(click())
                onView(withId(R.id.settingsBackupExportLayout)).check(matches(isDisplayed()))
                onView(withId(R.id.settingsBackupImportLayout)).check(matches(isDisplayed()))

                // SettingsViewModel's init block sets several MutableLiveData values synchronously,
                // which throws off the main thread - construct it on the main thread like the real
                // Fragment does via `lazyViewModel`.
                lateinit var viewModel: SettingsViewModel
                runOnMain { viewModel = app.appComponent.settingsModel().create() }
                var completedMessage: String? = null
                viewModel.onBackupCompleted { completedMessage = it; idlingResource.decrement() }
                viewModel.onBackupFailed { completedMessage = "FAILED: $it"; idlingResource.decrement() }

                // Export the seeded budget to a real file.
                val exportFile = File(app.cacheDir, "backup-export-test.json")
                runBackupOperationAndAwait { viewModel.exportBudgets(Uri.fromFile(exportFile)) }

                assertTrue(exportFile.exists())
                val exported = BackupJson.fromJson(exportFile.readText())
                val exportedBudget = exported.budgets.first { it.name == "Trip" }
                assertTrue(exportedBudget.spendings.any { it.comment == "Coffee" })

                // A budget with a name that doesn't exist yet is imported with fresh ids.
                val newBudgetFile = File(app.cacheDir, "backup-new-budget-test.json")
                newBudgetFile.writeText(BackupJson.toJson(freshBudgetBackup()))

                completedMessage = null
                runBackupOperationAndAwait { viewModel.importBudgets(Uri.fromFile(newBudgetFile)) }
                assertEquals(String.format(app.getString(R.string.message_backup_import_done), 1, 0), completedMessage)

                val importedBudget = db.budgetDao().getByName("Imported Trip")
                assertTrue(importedBudget != null)
                assertEquals(1, db.expenditureDao().getByBudgetId(importedBudget!!.id).size)
                assertEquals(1, db.spendingDao().getByBudgetId(importedBudget.id).size)

                // Re-importing a budget whose name already exists is skipped, not duplicated - twice in a row.
                repeat(2) {
                    completedMessage = null
                    runBackupOperationAndAwait { viewModel.importBudgets(Uri.fromFile(exportFile)) }
                    assertEquals(String.format(app.getString(R.string.message_backup_import_done), 0, 1), completedMessage)
                }
                assertEquals(1, db.budgetDao().getAll().count { it.name == "Trip" })
            }
        } finally {
            IdlingRegistry.getInstance().unregister(idlingResource)
        }
    }

    private fun runOnMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    /**
     * Runs a `SettingsViewModel` export/import call on the main thread, then performs a no-op
     * Espresso assertion purely to force Espresso to wait on [idlingResource] - which the call's
     * `onBackupCompleted`/`onBackupFailed` callback decrements - before returning.
     */
    private fun runBackupOperationAndAwait(action: () -> Unit) {
        idlingResource.increment()
        runOnMain(action)
        onView(withId(R.id.settingsBackupExportLayout)).check(matches(isDisplayed()))
    }

    private fun freshBudgetBackup() = BackupFile(
        exportedAt = LocalDateTime.now(),
        budgets = listOf(
            BudgetBackup(
                id = 999,
                name = "Imported Trip",
                plan = 20.0,
                fact = 0.0,
                startsOn = LocalDate.now(),
                lastDayAt = LocalDate.now().plusDays(6),
                comment = null,
                currency = "USD",
                expenditures = listOf(
                    ExpenditureBackup(id = 1, name = "Groceries", iconId = null, plan = 20.0, fact = 0.0, comment = "", displayOrder = 0)
                ),
                spendings = listOf(
                    SpendingBackup(
                        id = 2, comment = "Bread", date = LocalDateTime.now(), amount = 3.0, expenditureId = 1,
                        exchangeCurrency = null, exchangeRate = null, exchangeRawAmount = null, recurrence = "Never",
                        messages = emptyList()
                    )
                )
            )
        )
    )
}
