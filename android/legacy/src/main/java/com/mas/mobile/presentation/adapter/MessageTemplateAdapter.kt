package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.navigation.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.MessageTemplateListRowBinding
import com.mas.mobile.domain.message.MessageTemplate
import com.mas.mobile.presentation.activity.fragment.MessageTemplateListFragment
import com.mas.mobile.presentation.activity.fragment.MessageTemplateListFragmentDirections
import com.mas.mobile.presentation.viewmodel.validator.Action

class MessageTemplateAdapter(
    private val fragment: MessageTemplateListFragment
): BaseAdapter<MessageTemplate, MessageTemplateListRowBinding>(R.layout.message_template_list_row) {
    override fun bind(binding: MessageTemplateListRowBinding, item: MessageTemplate, prior: MessageTemplate?) {
        binding.messageTemplate = item
        binding.messageTemplateRow.setOnClickListener {
            val action = MessageTemplateListFragmentDirections.actionToMessageTemplate(Action.VIEW.name, item.id.value)
            it.findNavController().navigate(action)
        }
        binding.messageTemplateRow.setOnLongClickListener {
            val action = MessageTemplateListFragmentDirections.actionToMessageTemplate(Action.EDIT.name, item.id.value)
            it.findNavController().navigate(action)
            true
        }
    }

    override fun getBinding(view: View) = MessageTemplateListRowBinding.bind(view)
}