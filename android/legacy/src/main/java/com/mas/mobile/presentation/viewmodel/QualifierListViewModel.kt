package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.QualifierModified
import com.mas.mobile.domain.analytics.QualifierModified.Status
import com.mas.mobile.domain.message.Qualifier
import com.mas.mobile.domain.message.QualifierRepository
import com.mas.mobile.service.CoroutineService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class QualifierListViewModel @AssistedInject constructor(
    private val eventLogger: EventLogger,
    private val repository: QualifierRepository,
    private val coroutineService: CoroutineService,
    @Assisted private val type: Qualifier.Type,
): ListViewModel<Qualifier>(coroutineService, repository){
    private val newValues = MutableLiveData<List<Qualifier>>()

    val qualifiers = MediatorLiveData<List<Qualifier>>()

    init {
        val masterSource = repository.live.getQualifiers(type)

        qualifiers.addSource(masterSource) {
            qualifiers.value = it
        }

        qualifiers.addSource(newValues) {
            qualifiers.value = it.plus(masterSource.value!!.toList())
        }
    }

    fun addNew() {
        newValues.value = listOf(
            repository.create().also {
                it.type = type
            }
        )
    }

    fun save(item: Qualifier) = process(item) { qualifier ->
        val value = qualifier.value.trim()
        with(repository) {
            val result = getQualifiers(qualifier.type).firstOrNull { it.value.equals(value, true) } ?: qualifier
            result.value = value
            save(result)
        }
        eventLogger.log(QualifierModified(Status.Save, type))
    }

    override fun remove(item: Qualifier) = process(item) { qualifier ->
        repository.remove(qualifier)
        eventLogger.log(QualifierModified(Status.Save, type))
    }

    private fun process(item: Qualifier, handle: suspend (Qualifier) -> Unit) {
        if (item.value.isNotBlank()) {
            coroutineService.backgroundTask {
                handle(item)
            }
        }
        newValues.value = emptyList()
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted type: Qualifier.Type): QualifierListViewModel
    }
}