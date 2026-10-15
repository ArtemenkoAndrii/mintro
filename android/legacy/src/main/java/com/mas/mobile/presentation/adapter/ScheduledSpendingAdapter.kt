package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.ScheduledSpendingListRowBinding
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.presentation.activity.fragment.SpendingListFragment
import com.mas.mobile.presentation.activity.fragment.SpendingListFragmentDirections
import com.mas.mobile.presentation.viewmodel.validator.Action

class ScheduledSpendingAdapter(
    private val fragment: SpendingListFragment
): BaseAdapter<Spending, ScheduledSpendingListRowBinding>(R.layout.scheduled_spending_list_row) {

    override fun bind(binding: ScheduledSpendingListRowBinding, item: Spending, prior: Spending?) {
        val rowView = binding.spendingListRowLayout
        binding.spending = item

        binding.spendingRowIcon.setImageDrawable(fragment.getDrawable(item.expenditure.iconId))

        if (item.comment.isEmpty()) {
            binding.spendingListRowComment.visibility = View.GONE
        }

        binding.callback = View.OnClickListener { viewMenu ->
            val menu = PopupMenu(viewMenu.context, viewMenu)
            menu.inflate(R.menu.standard_row_menu)
            menu.setOnMenuItemClickListener {
                val spendingId = item.id.value
                val budgetId = item.expenditure.budgetId.value
                when (it.itemId) {
                    R.id.standard_row_menu_view -> {
                        val action = SpendingListFragmentDirections.actionToSpending(Action.VIEW.name, spendingId, budgetId)
                        rowView.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_edit -> {
                        val action = SpendingListFragmentDirections.actionToSpending(Action.EDIT.name, spendingId, budgetId)
                        rowView.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_clone -> {
                        val action = SpendingListFragmentDirections.actionToSpending(Action.CLONE.name, spendingId, budgetId)
                        rowView.findNavController().navigate(action)
                        true
                    }
                    R.id.standard_row_menu_remove -> {
                        with(fragment) {
                            showConfirmationDialog(getResourceService().messageAreYouSure()) {
                                listViewModel.remove(item)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            menu.show()
        }

        binding.spendingListRowLayout.setOnClickListener {
            val action = SpendingListFragmentDirections.actionToSpending(Action.VIEW.name, item.id.value)
            rowView.findNavController().navigate(action)
        }
        binding.spendingListRowLayout.setOnLongClickListener {
            val action = SpendingListFragmentDirections.actionToSpending(Action.EDIT.name, item.id.value)
            rowView.findNavController().navigate(action)
            true
        }
    }

    override fun getBinding(view: View) = ScheduledSpendingListRowBinding.bind(view)
}