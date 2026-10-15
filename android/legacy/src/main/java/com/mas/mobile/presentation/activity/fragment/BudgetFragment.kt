package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.BudgetFragmentBinding
import com.mas.mobile.presentation.activity.converter.TextDrawable
import com.mas.mobile.presentation.viewmodel.BudgetViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.util.DateTool

class BudgetFragment : ItemFragment<BudgetViewModel>() {
    private lateinit var binding: BudgetFragmentBinding
    private val args: BudgetFragmentArgs by navArgs()

    override val viewModel: BudgetViewModel by lazyViewModel {
        this.requireContext().appComponent.budgetViewModel()
            .create(action, args.budgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.budget_fragment, container, false)

        action = Action.valueOf(args.action)

        binding = BudgetFragmentBinding.bind(layout)
        binding.budget = viewModel
        binding.lifecycleOwner = this

        with(binding.budgetCurrencyLayout) {
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

        binding.budgetSaveButton.setOnClickListener {
            saveAndClose(viewModel)
        }

        binding.budgetLastDayAt.setOnClickListener {
            if (viewModel.isChangeable()) {
                showDateDialog(
                    startDate= viewModel.lastDayAtValue,
                    minDate = viewModel.startsOnValue
                ) {
                    binding.budgetLastDayAt.setText(DateTool.dateToString(it))
                }
            } else {
                showInfoDialog(getResourceService().budgetChangeDateMessage()) { }
            }
        }

        return layout
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.budget_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.budget_menu_edit -> {
                viewModel.enableEditing()
                action = Action.EDIT
                true
            }
            R.id.budget_menu_remove -> showConfirmationDialog {
                viewModel.remove()
                findNavController().popBackStack()
            }.let { true }
            R.id.budget_menu_expenditure_list -> go(BudgetFragmentDirections.actionToBudgetExpenditureList(args.budgetId)).let { true }
            R.id.budget_menu_spending_list-> go(BudgetListFragmentDirections.actionToBudgetSpendingList(args.budgetId)).let { true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
