package com.mas.mobile.presentation.adapter

import android.view.View
import androidx.navigation.findNavController
import com.mas.mobile.R
import com.mas.mobile.databinding.ExpenditureListRowBinding
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.presentation.activity.fragment.ExpenditureListFragment
import com.mas.mobile.presentation.activity.fragment.ExpenditureListFragmentDirections

import com.mas.mobile.presentation.viewmodel.NEW_ITEM
import com.mas.mobile.presentation.viewmodel.validator.Action

open class ExpenditureAdapter(
    private val fragment: ExpenditureListFragment
): BaseAdapter<Expenditure, ExpenditureListRowBinding>(R.layout.expenditure_list_row) {
    override fun bind(binding: ExpenditureListRowBinding, item: Expenditure, prior: Expenditure?) {
        binding.expenditureRowMenu.visibility = View.GONE
        binding.expenditureRowSpace.visibility = View.VISIBLE
        binding.expenditure = item

        binding.expenditureRowIcon.setImageDrawable(fragment.getDrawable(item.iconId))

        binding.expenditureRowLayout.setOnClickListener {
            ExpenditureListFragmentDirections.actionToExpenditureSpending(
                Action.ADD.name,
                NEW_ITEM,
                item.id.value
            ).also {
                binding.expenditureRowLayout.findNavController().navigate(it)
            }
        }

        binding.expenditureRowLayout.setOnLongClickListener {
            ExpenditureListFragmentDirections.actionToExpenditureSpendingList(
                expenditureId = item.id.value
            ).also {
                binding.expenditureRowLayout.findNavController().navigate(it)
            }
            true
        }
    }

    override fun getBinding(view: View) = ExpenditureListRowBinding.bind(view)
}