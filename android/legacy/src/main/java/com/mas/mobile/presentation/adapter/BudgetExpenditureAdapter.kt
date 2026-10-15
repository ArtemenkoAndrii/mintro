package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.ExpenditureListRowBinding
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.presentation.activity.fragment.BudgetExpenditureListFragmentDirections
import com.mas.mobile.presentation.activity.fragment.ExpenditureListFragment
import com.mas.mobile.presentation.viewmodel.validator.Action

class BudgetExpenditureAdapter(private val fragment: ExpenditureListFragment): ExpenditureAdapter(fragment) {
    override fun bind(binding: ExpenditureListRowBinding, item: Expenditure, prior: Expenditure?) {
        binding.expenditure = item

        binding.expenditureRowIcon.setImageDrawable(fragment.getDrawable(item.iconId))

        binding.callback = View.OnClickListener { viewMenu ->
            val menu = PopupMenu(viewMenu.context, viewMenu)
            menu.inflate(R.menu.standard_row_menu)
            menu.setOnMenuItemClickListener { menuItem ->
                val controller = binding.expenditureRowLayout.findNavController()
                when (menuItem.itemId) {
                    R.id.standard_row_menu_edit ->
                        BudgetExpenditureListFragmentDirections.actionToBudgetExpenditure(
                            Action.EDIT.name,
                            item.id.value,
                            fragment.listViewModel.budgetId
                        ).also { controller.navigate(it) }
                    R.id.standard_row_menu_clone ->
                        BudgetExpenditureListFragmentDirections.actionToBudgetExpenditure(
                            Action.CLONE.name,
                            item.id.value,
                            fragment.listViewModel.budgetId
                        ).also { controller.navigate(it) }
                    R.id.standard_row_menu_remove -> {
                        with(fragment) {
                            showConfirmationDialog(getResourceService().messageAreYouSure()) {
                                listViewModel.remove(item)
                            }
                        }
                    }
                    else ->
                        BudgetExpenditureListFragmentDirections.actionToBudgetExpenditure(
                            Action.VIEW.name,
                            item.id.value,
                            fragment.listViewModel.budgetId
                        ).also { controller.navigate(it) }
                }
                true
            }
            menu.show()
        }

        binding.expenditureRowLayout.setOnClickListener { _ ->
            BudgetExpenditureListFragmentDirections.actionToBudgetExpenditure(
                Action.VIEW.name,
                item.id.value,
                fragment.listViewModel.budgetId
            ).also { binding.expenditureRowLayout.findNavController().navigate(it) }
        }

        binding.expenditureRowLayout.setOnLongClickListener {
            binding.expenditureRowLayout.findNavController().navigate(
                BudgetExpenditureListFragmentDirections.actionToExpenditureSpendingList(
                    expenditureId = item.id.value
                )
            )
            true
        }
    }
}
