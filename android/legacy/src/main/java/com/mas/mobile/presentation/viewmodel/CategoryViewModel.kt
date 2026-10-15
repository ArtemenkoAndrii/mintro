package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.analytics.CategoryModified
import com.mas.mobile.domain.analytics.CategoryModified.Status
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.budget.Category
import com.mas.mobile.domain.budget.CategoryId
import com.mas.mobile.domain.budget.CategoryRepository
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.domain.budget.Merchant
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.presentation.viewmodel.validator.FieldValidator
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CategoryViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    private val eventLogger: EventLogger,
    private val repository: CategoryRepository,
    private val fieldValidator: FieldValidator,
    @Assisted("categoryId") private val categoryId: Int,
    @Assisted private val action: Action
) : ItemViewModel<Category>(coroutineService, repository) {
    override val model: Category = loadModel()

    val name = MutableLiveData<String>()
    val nameError = MutableLiveData<String>()
    val icon = MutableLiveData<IconId?>()
    val plan = MutableLiveData<Double>()
    val isActive = MutableLiveData<Boolean>()
    val description = MutableLiveData<String>()
    val merchants = MutableLiveData<MutableSet<String>>()
    val merchantsError = MutableLiveData<String>()
    val currentMerchant = MutableLiveData<String>()

    fun addMerchant() {
        val value = currentMerchant.value?.trim() ?: ""

        val validationResult = fieldValidator.minLength(value, VAL_MIN_LENGTH)
        merchantsError.value = validationResult
        if (validationResult.isBlank()) {
            merchants.value = merchants.value?.also { it.add(value) }
            currentMerchant.value = ""
        }
    }

    fun removeMerchant(value: String) {
        merchants.value = merchants.value?.also { it.remove(value) }
    }

    init {
        initProperties(model)
    }

    private fun initProperties(item: Category) {
        icon.value = item.iconId
        icon.observeForever {
            item.iconId = it
        }

        nameError.value = FieldValidator.NO_ERRORS
        name.value = item.name
        name.observeForever {
            item.name = it.trim()

            validateOnSave(nameError) {
                or(
                    fieldValidator.minLength(item.name, VAL_MIN_LENGTH),
                    fieldValidator.alreadyExists(ifExists())
                )
            }
        }

        plan.value = item.plan
        plan.observeForever {
            item.plan = it
        }

        isActive.value = item.isActive
        isActive.observeForever {
            item.isActive = it
        }

        description.value = item.description
        description.observeForever {
            item.description = it
        }

        merchantsError.value = FieldValidator.NO_ERRORS
        merchants.value = item.merchants.map { it.value }.toMutableSet()
        merchants.observeForever {
            item.merchants = it.map { value -> Merchant(value) }.toMutableList()
        }
    }

    private fun ifExists(): Boolean =
        repository.getAll().any {
            model.name.equals(it.name, true) && model.id != it.id
        }

    private fun loadModel() =
        when(action) {
            Action.ADD -> repository.create().also { enableEditing() }
            Action.VIEW -> repository.getById(CategoryId(categoryId))
            Action.EDIT -> repository.getById(CategoryId(categoryId)).also { enableEditing() }
            Action.CLONE -> {
                val origin = repository.getById(CategoryId(categoryId))
                enableEditing()
                repository.create().also {
                    if (origin != null) {
                        it.name = origin.name
                        it.plan = origin.plan
                        it.isActive = origin.isActive
                        it.description = origin.description
                    }
                }
            }
            else -> throw ActionNotSupportedException("BudgetViewModel does not support $action action")
        } ?: throw ItemNotFoundException("Item not found id=$categoryId")

    override suspend fun doSave() {
        if (action == Action.ADD || action == Action.CLONE) {
            model.displayOrder = (repository.getAll().maxOfOrNull { it.displayOrder } ?: 0) + 1
        }
        super.doSave()
        eventLogger.log(CategoryModified(Status.Save))
    }

    override suspend fun doRemove() {
        super.doRemove()
        eventLogger.log(CategoryModified(Status.Remove))
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("categoryId") categoryId: Int,
                   action: Action): CategoryViewModel
    }

    companion object {
        const val VAL_MIN_LENGTH = 3
    }
}