package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.BudgetListFragmentBinding
import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.presentation.adapter.BudgetAdapter
import com.mas.mobile.presentation.viewmodel.BudgetListViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action

class  BudgetListFragment: ListFragment(), ListMenu<Budget> {
    private val listViewModel: BudgetListViewModel by lazyViewModel {
        this.requireContext().appComponent.budgetListViewModel().create()
    }
    private lateinit var binding: BudgetListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.budget_list_fragment, container, false)

        val adapter = BudgetAdapter(this)
        binding = BudgetListFragmentBinding.bind(layout)
        binding.budgetList.adapter = adapter
        binding.budgetList.layoutManager = LinearLayoutManager(this.requireContext())

        listViewModel.budgets.observe(viewLifecycleOwner) { budget ->
            adapter.setItems(budget)
        }

        binding.budgetAddBtn.setOnClickListener {
            listViewModel.createBudget()
            //this.findNavController().navigate(resolveAddButtonDestination())
        }

        return layout
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.budget_list_menu_add -> go(resolveAddButtonDestination())
            R.id.budget_list_menu_settings -> go(BudgetListFragmentDirections.actionToBudgetSettings())
            R.id.budget_analytics -> go(R.id.action_to_analytics)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun resolveAddButtonDestination() = BudgetListFragmentDirections.actionToBudget(Action.ADD.name)

    override fun onRowMenuSelected(menuItem: MenuItem, item: Budget): Boolean {
        when (menuItem.itemId) {
            R.id.budget_row_menu_edit -> go(BudgetListFragmentDirections.actionToBudget(Action.EDIT.name, item.id.value))
            R.id.budget_row_menu_expenditure_list -> go(BudgetListFragmentDirections.actionToBudgetExpenditureList(item.id.value))
            R.id.budget_row_menu_spending_list -> go(BudgetListFragmentDirections.actionToBudgetSpendingList(item.id.value))
            R.id.budget_row_menu_remove -> deleteItem(item)
        }
        return false
    }

    private fun deleteItem(item: Budget) {
        if (listViewModel.isChangeable(item)) {
            showConfirmationDialog {
                listViewModel.remove(item)
            }
        } else {
            showInfoDialog(getResourceService().budgetRemoveMessage()) {}
        }
    }
}
