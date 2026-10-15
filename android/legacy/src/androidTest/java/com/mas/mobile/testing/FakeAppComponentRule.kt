package com.mas.mobile.testing

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import com.mas.mobile.DaggerAppComponent
import com.mas.mobile.MasApplication
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rebuilds [MasApplication.appComponent] with [TestPersistenceModule] (in-memory Room DB) and
 * [TestExternalServicesModule] (fake GPT/currency/task collaborators) before the test body runs.
 *
 * `MasApplication.appComponent` is a `lateinit var`, so this only needs to reassign it - no
 * custom test `Application`/`AndroidJUnitRunner` is required. Must run before any
 * `ActivityScenario`/`FragmentScenario` is launched, so put it first in a `RuleChain`
 * (or simply declare it as the outermost `@Rule`, since JUnit4 applies rules from the
 * outside in).
 */
class FakeAppComponentRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val app = ApplicationProvider.getApplicationContext<MasApplication>()
            val persistenceModule = TestPersistenceModule()
            app.appComponent = DaggerAppComponent.builder()
                .context(app)
                .persistenceModule(persistenceModule)
                .externalServicesModule(TestExternalServicesModule())
                .build()

            IdlingRegistry.getInstance().register(persistenceModule.idlingResource)
            try {
                base.evaluate()
            } finally {
                IdlingRegistry.getInstance().unregister(persistenceModule.idlingResource)
            }
        }
    }
}
