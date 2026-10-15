package com.mas.mobile.testing

import com.mas.mobile.service.TaskService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * [TaskService] for instrumented tests that runs both [backgroundTask] and [blockingTask]
 * synchronously via `runBlocking`, instead of the real `Dispatchers.IO`-backed
 * `CoroutineService`. Espresso's default idling already waits out the main-thread message
 * queue, so making "background" work run synchronously on the calling thread is enough to
 * keep tests deterministic without a custom IdlingResource.
 */
class SynchronousTaskService : TaskService {
    override fun backgroundTask(wrapper: suspend (context: CoroutineScope) -> Unit) {
        runBlocking { wrapper(this) }
    }

    override fun blockingTask(wrapper: suspend (context: CoroutineScope) -> Unit) {
        runBlocking { wrapper(this) }
    }
}
