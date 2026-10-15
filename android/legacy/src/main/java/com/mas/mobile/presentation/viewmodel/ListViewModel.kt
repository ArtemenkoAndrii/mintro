package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mas.mobile.domain.Repository
import com.mas.mobile.service.CoroutineService

abstract class ListViewModel<T>(
    private val coroutineService: CoroutineService,
    private val repository: Repository<T>,
) : ViewModel() {

    open fun remove(item: T) {
        coroutineService.backgroundTask {
            repository.remove(item)
        }
    }
}