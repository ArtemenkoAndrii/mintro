package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.MessageTemplateFragmentBinding
import com.mas.mobile.presentation.activity.converter.TextDrawable
import com.mas.mobile.presentation.viewmodel.MessageTemplateViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action

class MessageTemplateFragment: ItemFragment<MessageTemplateViewModel>() {
    private val args: MessageTemplateFragmentArgs by navArgs()
    private lateinit var binding: MessageTemplateFragmentBinding

    override val viewModel: MessageTemplateViewModel by lazyViewModel {
        this.requireContext().appComponent.messageTemplateViewModel()
            .create(args.messageTemplateId, action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.message_template_fragment, container, false)

        action = Action.valueOf(args.action)

        binding = MessageTemplateFragmentBinding.bind(layout)
        binding.model = viewModel
        binding.lifecycleOwner = this

        with(binding.messageTemplateCurrencyLayout) {
            isEndIconVisible = true
            viewModel.currency.observeForever {
                endIconDrawable = TextDrawable(requireContext(), it.symbol)
            }

            val onClick = View.OnClickListener {
                showCurrencyPicker { currency ->
                    viewModel.currency.value = currency
                    clearFocus()
                }
            }
            editText?.setOnClickListener(onClick)
            setEndIconOnClickListener(onClick)
        }

        binding.messageTemplateSaveButton.setOnClickListener {
            saveAndClose(viewModel)
        }

        return layout
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
    }
}