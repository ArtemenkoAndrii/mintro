package com.mas.mobile.testing

import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.Executor

/**
 * Wraps [delegate] so every task run through it is tracked by [idlingResource]. Room's
 * `LiveData`-returning DAO queries execute on the database's query/transaction executor, not the
 * main thread, so Espresso's default idling (which only tracks the main thread's message queue)
 * doesn't wait for them - a fresh query can still be in flight when a test's next `perform()`/
 * `check()` runs, causing a flaky "no views found" failure. Installing this as Room's query and
 * transaction executor (see [TestPersistenceModule]) closes that gap.
 */
class IdlingResourceExecutor(
    private val delegate: Executor,
    private val idlingResource: CountingIdlingResource
) : Executor {
    override fun execute(command: Runnable) {
        idlingResource.increment()
        delegate.execute {
            try {
                command.run()
            } finally {
                idlingResource.decrement()
            }
        }
    }
}
