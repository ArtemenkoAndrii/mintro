package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.SpendingListRowBinding
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.presentation.activity.fragment.SpendingListFragment
import com.mas.mobile.presentation.activity.fragment.SpendingListFragmentDirections
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.util.DateTool
import java.time.LocalDate
import java.time.LocalDateTime

class SpendingAdapter(
    private val fragment: SpendingListFragment
): BaseAdapter<Spending, SpendingListRowBinding>(R.layout.spending_list_row) {
    private val today = fragment.getResourceService().constantToday()
    private val yesterday = fragment.getResourceService().constantYesterday()

    override fun bind(binding: SpendingListRowBinding, item: Spending, prior: Spending?) {
        val rowView = binding.spendingListRowLayout
        binding.spending = item

        binding.spendingRowIcon.setImageDrawable(fragment.getDrawable(item.expenditure.iconId))

        if (prior?.date?.toLocalDate() == item.date.toLocalDate()) {
            binding.spendingListRowDate.visibility = View.GONE
        } else {
            binding.spendingListRowDate.visibility = View.VISIBLE
        }

        if (item.comment.isEmpty()) {
            binding.spendingListRowComment.visibility = View.GONE
        }

        binding.spendingListRowDate.setText(calcRelativeDate(item.date))
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

    private fun calcRelativeDate(value: LocalDateTime): String {
        val date = value.toLocalDate()
        return when {
            date.equals(LocalDate.now()) -> this.today
            date.equals(LocalDate.now().minusDays(1)) -> this.yesterday
            else -> DateTool.dateToAbbreviatedString(date)
        }
    }

    override fun getBinding(view: View) = SpendingListRowBinding.bind(view)
}