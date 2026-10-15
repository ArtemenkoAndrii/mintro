package com.mas.mobile.presentation.activity.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.SpendingListFragmentBinding
import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.presentation.adapter.ScheduledSpendingAdapter
import com.mas.mobile.presentation.adapter.SpendingAdapter
import com.mas.mobile.presentation.viewmodel.NEW_ITEM
import com.mas.mobile.presentation.viewmodel.SpendingListViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action

open class SpendingListFragment : ListFragment() {
    protected val args: SpendingListFragmentArgs by navArgs()
    protected lateinit var binding: SpendingListFragmentBinding

    val listViewModel: SpendingListViewModel by lazyViewModel {
        this.requireContext().appComponent.spendingListViewModel().create(args.budgetId, args.expenditureId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.spending_list_fragment, container, false)

        val adapter = SpendingAdapter(this)
        binding = SpendingListFragmentBinding.bind(layout)
        binding.spendingList.adapter = adapter
        binding.spendingList.layoutManager = LinearLayoutManager(this.requireContext())

        listViewModel.spendings.observe(viewLifecycleOwner) { spending ->
            adapter.setItems(spending)
        }

        binding.spendingAddBtn.setOnClickListener {
            val action = resolveAddButtonDestination()
            this.findNavController().navigate(action)
        }

        return layout
    }

    fun getDrawable(id: IconId?): Drawable {
        return this.getIconPack().icons[id?.value]?.drawable
            ?: AppCompatResources.getDrawable(requireContext(), R.drawable.ic_circle)!!
    }

    override fun resolveAddButtonDestination() =
        SpendingListFragmentDirections.actionToSpending(Action.ADD.name)
}

class BudgetSpendingListFragment: SpendingListFragment() {

    override fun resolveAddButtonDestination(): NavDirections {
        return BudgetSpendingListFragmentDirections.actionToSpending(Action.ADD.name, NEW_ITEM, -1, args.budgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listViewModel.budget.observe(viewLifecycleOwner) { budget ->
            setTitle { "${budget.name} $it" }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroy() {
        showBottomMenu()
        super.onDestroy()
    }
}

class ExpenditureSpendingListFragment: SpendingListFragment() {

    override fun resolveAddButtonDestination(): NavDirections {
        return BudgetSpendingListFragmentDirections.actionToSpending(Action.ADD.name, NEW_ITEM, args.expenditureId, args.budgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        hideBottomMenu()
        setTitle { listViewModel.expenditure?.name ?: "" }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroy() {
        showBottomMenu()
        super.onDestroy()
    }
}

class ScheduledSpendingListFragment: SpendingListFragment() {
    override fun resolveAddButtonDestination(): NavDirections {
        return BudgetSpendingListFragmentDirections.actionToSpending(Action.ADD.name, NEW_ITEM, -1, Budget.SCHEDULED_BUDGET_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listViewModel.budget.observe(viewLifecycleOwner) { budget ->
            setTitle { getResourceService().titleScheduled() }
        }

        val layout = super.onCreateView(inflater, container, savedInstanceState)
        val adapter = ScheduledSpendingAdapter(this)
        binding.spendingList.adapter = adapter
        binding.spendingList.layoutManager = LinearLayoutManager(this.requireContext())
        listViewModel.spendings.observe(viewLifecycleOwner) { spending ->
            adapter.setItems(spending)
        }

        return layout
    }
}