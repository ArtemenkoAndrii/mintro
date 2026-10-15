package com.mas.mobile.presentation.viewmodel

import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import androidx.lifecycle.MutableLiveData
import com.mas.mobile.domain.analytics.EventLogger
import com.mas.mobile.domain.analytics.MessageTemplateModified
import com.mas.mobile.domain.analytics.MessageTemplateModified.Status
import com.mas.mobile.domain.message.MessageTemplate
import com.mas.mobile.domain.message.MessageTemplateId
import com.mas.mobile.domain.message.MessageTemplateRepository
import com.mas.mobile.domain.message.Pattern
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.service.CoroutineService
import com.mas.mobile.service.ResourceService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.Currency

class MessageTemplateViewModel @AssistedInject constructor(
    coroutineService: CoroutineService,
    val repository: MessageTemplateRepository,
    private val resources: ResourceService,
    private val eventLogger: EventLogger,
    @Assisted private val action: Action,
    @Assisted private val messageTemplateId: Int = NEW_ITEM,
) : ItemViewModel<MessageTemplate>(coroutineService, repository) {
    private var patternValue: String = ""

    override val model: MessageTemplate = loadModel()

    var sender = MutableLiveData<String>()
    var pattern = MutableLiveData<String>()
    var patternError = MutableLiveData<String>()
    var example = MutableLiveData<SpannableString>()
    var currency = MutableLiveData<Currency>()
    var isEnabled = MutableLiveData<Boolean>()

    init {
        sender.value = model.sender
        sender.observeForever {
            model.sender = it
        }

        pattern.value = model.pattern.value
        patternError.value = ""
        pattern.observeForever {
            if (patternValue != it) {
                patternValue = it
                example.value = toSpannable(model.example)
            }
        }

        example.observeForever {
            val value = it.toString()
            if (value != model.example) {
                model.example = value
                example.value = toSpannable(model.example)
            }
        }

        currency.value = model.currency
        currency.observeForever {
            model.currency = it
        }

        isEnabled.value = model.isEnabled
        isEnabled.observeForever {
            model.isEnabled = it
        }

        validateOnSave(patternError) {
            try {
                // TODO: Fix implicit saving to model
                model.pattern = Pattern(patternValue)

                if (model.pattern.parse(model.example) is Pattern.Data) {
                    ""
                } else {
                    resources.validatorPatternFailed()
                }
            } catch (e: IllegalArgumentException) {
                e.message ?: e.toString()
            }
        }
    }

    private fun toSpannable(value: String): SpannableString {
        val span = SpannableString(value)
        try {
            when (val result = Pattern(patternValue).parse(model.example)) {
                is Pattern.Data -> span.mark(result)
                else -> span.fail()
            }
        } catch (e: Throwable) {
            span.fail()
        }

        return span
    }

    private fun SpannableString.mark(data: Pattern.Data) {
        this.setSpan(
            BackgroundColorSpan(Color.GREEN),
            data.indexes.amountStart,
            data.indexes.amountEnd + 1,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (data.indexes.merchantStart > -1) {
            this.setSpan(
                BackgroundColorSpan(Color.GREEN),
                data.indexes.merchantStart,
                data.indexes.merchantEnd + 1,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun SpannableString.fail() {
        this.setSpan(
            BackgroundColorSpan(Color.RED),
            0,
            length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun loadModel() =
        with(repository) {
            when(action) {
                Action.ADD -> create().also { enableEditing() }
                Action.VIEW -> getById(MessageTemplateId(messageTemplateId))
                Action.EDIT -> getById(MessageTemplateId(messageTemplateId)).also { enableEditing() }
                Action.CLONE -> {
                    getById(MessageTemplateId(messageTemplateId))
                        ?.let { clone(it) } ?: create()
                        .also { enableEditing() }
                }
                else -> throw ActionNotSupportedException("MessageTemplateId does not support $action action")
            } ?: throw ItemNotFoundException("Item not found id=$messageTemplateId")
        }

    override suspend fun doSave() {
        super.doSave()
        eventLogger.log(MessageTemplateModified(Status.Save))
    }

    override suspend fun doRemove() {
        super.doRemove()
        eventLogger.log(MessageTemplateModified(Status.Remove))
    }

    @AssistedFactory
    interface Factory {
        fun create(messageTemplateId: Int = NEW_ITEM, action: Action): MessageTemplateViewModel
    }
}
