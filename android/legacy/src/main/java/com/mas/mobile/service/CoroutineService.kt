package com.mas.mobile.service

import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoroutineService @Inject constructor(
    private val errorHandler: ErrorHandler
) : TaskService {
    override fun backgroundTask(wrapper: suspend (context: CoroutineScope) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch(coroutineErrorHandler) {
            wrapper(this)
        }
    }

    override fun blockingTask(wrapper: suspend (context: CoroutineScope) -> Unit) {
        runBlocking {
            wrapper(this)
        }
    }

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        errorHandler.handleAndNotify(throwable)
    }
}
