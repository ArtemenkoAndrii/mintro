package com.mas.mobile.domain.budget

import com.mas.mobile.service.TaskService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryService @Inject constructor(
    private val coroutineService: TaskService,
    val repository: CategoryRepository
) {
    fun findCategoryByMerchant(merchant: Merchant): Category? {
        return repository.getAll()
            .firstOrNull { it.merchants.containsIgnoreCase(merchant.value) }
    }

    fun findCategoryByName(name: String): Category? {
        return repository.getAll()
            .firstOrNull { it.name.equals(name, true) }
    }

    fun reorder(old: List<Category>, new: List<Category>) {
        if (old.size != new.size) return

        val itemsWithNewDisplayOrder = old.indices
            .filter { new[it].id != old[it].id }
            .associate { new[it] to old[it].displayOrder }

        coroutineService.backgroundTask {
            itemsWithNewDisplayOrder.forEach { (category, displayOrder) ->
                category.displayOrder = displayOrder
                repository.save(category)
            }
        }
    }

    private fun List<Merchant>.containsIgnoreCase(value: String): Boolean =
        this.any { it.value.equals(value, true) }
}