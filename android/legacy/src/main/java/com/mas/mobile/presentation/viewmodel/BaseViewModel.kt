package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.repository.db.BaseRepository
import com.mas.mobile.service.CoroutineService

const val NEW_ITEM = -1

abstract class BaseViewModel<T: Any>(
    private val id: Int = -1,
    private val action: Action = Action.VIEW,
    private val coroutineService: CoroutineService
) : ViewModel() {
    private var allowValidation = false
    private val validators = mutableMapOf<Int, () -> Boolean>()
    private lateinit var model: T
    private var isNew = (action == Action.ADD || action == Action.CLONE)

    var isEditable = MutableLiveData(action != Action.VIEW)

    fun load() {
        this.model = when (action) {
            Action.ADD -> getRepository().createNew()
            Action.CLONE -> with(getRepository()) {
                this.getById(id)?.let {
                    this.clone(it)
                }
            }
            else -> getRepository().getById(id)
        } ?: throw Exception("Can't find item with id = $id")

        afterLoad(this.model)
        allowValidation = true
    }

    fun remove() {
        coroutineService.backgroundTask {
            val item = this@BaseViewModel.model
            getRepository().delete(item)
            afterRemove(item)
        }
    }

    abstract fun getRepository(): BaseRepository<T>
    abstract fun afterLoad(item: T)
    open fun beforeSave(item: T) {}
    open suspend fun afterSave(item: T) {}
    open suspend fun afterRemove(item: T) {}

    fun save(): Boolean {
        if (!isValid()) {
            return false
        }

        beforeSave(this.model)
        coroutineService.backgroundTask {
            doSave()
        }

        return true
    }

    fun validateOnChange(field: MutableLiveData<String>, validator: () -> String) =
        registerAndRun(field.hashCode(), field, validator)

    fun validateOnSave(field: MutableLiveData<String>, validator: () -> String) =
        registerAndRun(validator.hashCode(), field, validator)

    private fun registerAndRun(key: Int, field: MutableLiveData<String>, validator: () -> String) {
        validators[key] = {
            field.value = validator()
            field.value == NO_ERRORS
        }
        if (allowValidation && action != Action.VIEW) {
            validators[key]?.invoke()
        }
    }

    fun enableEditing() {
        isEditable.value = true
    }

    fun disableEditing() {
        isEditable.value = false
    }

    private fun isValid() =
        validators.values.firstOrNull { !it.invoke() } == null

    private suspend fun doSave() {
        if (this.isNew) {
            getRepository().insert(this.model).also {
                this.isNew = false
                reload(it.toInt())
            }
        } else {
            getRepository().update(this.model)
            reload(this.id)
        }
        afterSave(this.model)
    }

    private fun reload(id: Int) {
        this.getRepository().getById(id)?.let {
            this.model = it
        }
    }

    companion object {
        const val NO_ERRORS = ""
    }
}
