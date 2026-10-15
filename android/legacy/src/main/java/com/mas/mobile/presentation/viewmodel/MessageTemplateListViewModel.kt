package com.mas.mobile.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mas.mobile.domain.message.MessageTemplateRepository
import com.mas.mobile.domain.settings.SettingsService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class MessageTemplateListViewModel @AssistedInject constructor(
    repository: MessageTemplateRepository,
    settingsService: SettingsService,
    @Assisted param: String
): ViewModel() {
    val senders = repository.live.getAll()
    val autodetect = MutableLiveData<Boolean>()

    init {
        autodetect.value = settingsService.autodetect
        autodetect.observeForever {
            settingsService.autodetect = it
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(param: String): MessageTemplateListViewModel
    }
}
