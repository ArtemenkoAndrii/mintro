package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mas.mobile.repository.db.BaseRepository
import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class BaseListViewModel<T: Any>(
    private val coroutineService: CoroutineService
): ViewModel() {
    abstract fun getRepository(): BaseRepository<T>

    fun remove(item: T) {
        coroutineService.backgroundTask {
            getRepository().delete(item)
            afterRemove(item)
        }
    }

    open suspend fun afterRemove(item: T) {}
}
