package com.mas.mobile.service

import kotlinx.coroutines.CoroutineScope

interface TaskService {
    fun backgroundTask(wrapper: suspend (context: CoroutineScope) -> Unit)
    fun blockingTask(wrapper: suspend (context: CoroutineScope) -> Unit)
}