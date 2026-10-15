package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mas.mobile.domain.Repository
import com.mas.mobile.service.CoroutineService

abstract class ItemViewModel<T>(
    private val coroutineService: CoroutineService,
    private val repository: Repository<T>,
) : ViewModel() {
    private val validators = mutableMapOf<Int, () -> Boolean>()
    private var validationAllowed = false

    abstract val model: T

    val isEditable = MutableLiveData<Boolean>().also { it.value = false }

    fun save(): Boolean {
        if (!isValid()) {
            return false
        }

        coroutineService.backgroundTask {
            doSave()
        }

        return true
    }

    fun remove() {
        coroutineService.backgroundTask {
            doRemove()
        }
    }

    fun enableEditing() {
        isEditable.value = true
    }

    fun disableEditing() {
        isEditable.value = false
    }

    open suspend fun doSave() {
        repository.save(model)
    }

    open suspend fun doRemove() {
        repository.remove(model)
    }

    fun validateOnChange(field: MutableLiveData<String>, validator: () -> String) =
        registerAndRun(field.hashCode(), field, validator)

    fun validateOnSave(field: MutableLiveData<String>, validator: () -> String) =
        registerAndRun(validator.hashCode(), field, validator)

    fun or(vararg results: String): String {
        for (str in results) {
            if (str.isNotEmpty()) {
                return str
            }
        }
        return ""
    }

    private fun registerAndRun(key: Int, field: MutableLiveData<String>, validator: () -> String) {
        validators[key] = {
            field.value = validator()
            field.value == BaseViewModel.NO_ERRORS
        }
        if (validationAllowed) {
            validators[key]?.invoke()
        }
    }

    fun isValid() = validators.values.any { !it() }.not()
}