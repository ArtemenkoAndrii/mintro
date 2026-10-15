package com.mas.mobile.domain.settings

import com.mas.mobile.service.TaskService
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeferrableActionService @Inject constructor(
    private val repository: DeferrableActionRepository,
    private val coroutineService: TaskService
) {
    fun isDeferred(key: DeferrableAction.Key): Boolean =
        repository.getByKey(key)?.isDeferred() ?: false

    fun defer(key: DeferrableAction.Key) {
        val actionToSave = repository.getByKey(key) ?: DeferrableAction(key)
        actionToSave.defer()
        coroutineService.backgroundTask {
            repository.save(actionToSave)
            repository.purge(LocalDateTime.now().minusDays(LIMIT))
        }
    }

    fun remove(key: DeferrableAction.Key) {
        coroutineService.backgroundTask {
            repository.getByKey(key)?.let { action ->
                repository.remove(action)
            }
        }
    }

    companion object {
        const val LIMIT = 30L
    }
}