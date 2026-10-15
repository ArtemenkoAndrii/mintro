package com.mas.mobile.presentation.viewmodel

import com.mas.mobile.domain.budget.Category
import com.mas.mobile.domain.budget.CategoryService
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CategoryListViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    val service: CategoryService,
    @Assisted("budgetId") budgetId: Int = -1,
) : ListViewModel<Category>(coroutineService, service.repository) {
    val categories = service.repository.live.getAll()

    fun reorder(old: List<Category>, new: List<Category>) {
        service.reorder(old, new)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("budgetId") budgetId: Int = -1
        ): CategoryListViewModel
    }
}