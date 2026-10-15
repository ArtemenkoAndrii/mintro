package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.SpendingFragmentBinding
import com.mas.mobile.presentation.activity.binding.setRecurrenceText
import com.mas.mobile.presentation.activity.converter.TextDrawable
import com.mas.mobile.presentation.viewmodel.SpendingViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action


class SpendingFragment : ItemFragment<SpendingViewModel>() {
    private lateinit var binding: SpendingFragmentBinding
    private val args: SpendingFragmentArgs by navArgs()

    override val viewModel: SpendingViewModel by lazyViewModel {
        this.requireContext().appComponent.spendingViewModel().create(
            args.spendingId,
            args.expenditureId,
            args.budgetId,
            args.messageId,
            action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.spending_fragment, container, false)

        action = Action.valueOf(args.action)

        binding = SpendingFragmentBinding.bind(layout)
        binding.model = viewModel
        binding.lifecycleOwner = this

        binding.spendingSaveButton.setOnClickListener {
            if (viewModel.hasUnsavedMerchant()) {
                val dialogText = getResourceService().messageLinkMerchant(
                    viewModel.getMerchant(), viewModel.getCategory())
                val onConfirm = {
                    saveAndClose(viewModel)
                }
                val onCancel = {
                    viewModel.skipSavingMerchant()
                    saveAndClose(viewModel)
                }
                showConfirmationDialog(dialogText, onConfirm, onCancel)
            } else {
                saveAndClose(viewModel)
            }
        }

        binding.messageParsingButton.setOnClickListener {
            inProgress(true)
            viewModel.loadFromRecommended { success ->
                inProgress(false)
                if (!success) {
                    showInfoDialog(this.getResourceService().messageSpendingNotFound()) {}
                }
            }
        }

        binding.messageParsingCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.spendingDate.setOnClickListener {
            showDateTimeDialog(binding.spendingDate)
        }

        ArrayAdapter(this.requireContext(),
            R.layout.autocomplete_item,
            viewModel.availableExpenditures.toList()).also {
            binding.spendingExpenditure.setAdapter(it)
        }


        val recurrences = viewModel.availabilityRecurrences.values.toList()
        binding.spendingRecurrences.setOnClickListener {
            showOptionsPicker(getResourceService().hintSpendingRecurrences(), recurrences, recurrences[0]) {
                viewModel.recurrence.value = it
            }
        }

        viewModel.recurrence.observeForever {
            setRecurrenceText(binding.spendingRecurrences, viewModel.model)
        }
        viewModel.scheduledDate.observeForever {
            setRecurrenceText(binding.spendingRecurrences, viewModel.model)
        }


        viewModel.discoverMode.observeForever {
            binding.spendingSaveButton.requestFocus()
        }

        with(binding.spendingAmountLayout) {
            isEndIconVisible = true
            endIconDrawable = TextDrawable(requireContext(), viewModel.getCurrencyFromBudget().symbol)
            setEndIconOnClickListener {
                showCurrencyPicker { currency ->
                    viewModel.exchangeCurrency.value = currency
                    clearFocus()
                }
            }
        }

        with(binding.spendingForeignAmountLayout) {
            isEndIconVisible = true
            viewModel.exchangeCurrency.observeForever {
                endIconDrawable = TextDrawable(requireContext(), it?.symbol.orEmpty())
            }
            setEndIconOnClickListener {
                showCurrencyPicker { currency ->
                    viewModel.exchangeCurrency.value = currency
                    clearFocus()
                }
            }
        }

        return layout
    }

    private fun inProgress(enabled: Boolean) {
        if (enabled) {
            binding.messageParsingProgressBar.visibility = View.VISIBLE
            binding.messageParsingButton.text = ""
            binding.messageParsingButton.isClickable = false
        } else {
            binding.messageParsingProgressBar.visibility = View.GONE
            binding.messageParsingButton.text = getResourceService().btnSpendingParseMessage()
            binding.messageParsingButton.isClickable = true
        }
    }
}
