package com.mas.mobile.presentation.activity.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.ExpenditureListFragmentBinding
import com.mas.mobile.domain.budget.IconId
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.adapter.BudgetExpenditureAdapter
import com.mas.mobile.presentation.adapter.ExpenditureAdapter
import com.mas.mobile.presentation.viewmodel.ExpenditureListViewModel
import com.mas.mobile.presentation.viewmodel.NEW_ITEM
import com.mas.mobile.presentation.viewmodel.validator.Action

open class ExpenditureListFragment : ListFragment() {
    private val args: ExpenditureListFragmentArgs by navArgs()
    protected lateinit var binding: ExpenditureListFragmentBinding

    val listViewModel: ExpenditureListViewModel by lazyViewModel {
        this.requireContext().appComponent.expenditureListViewModel().create(args.budgetId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.expenditure_list_fragment, container, false)

        listViewModel.budget.observe(viewLifecycleOwner) { budget ->
            setTitle { budget.name }
        }

        val adapter = getAdapter()
        binding = ExpenditureListFragmentBinding.bind(layout)
        binding.expenditureList.adapter = adapter
        binding.expenditureList.layoutManager = LinearLayoutManager(this.requireContext())
        binding.model = listViewModel
        binding.lifecycleOwner = this

        listViewModel.expenditures.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }

        binding.expenditureAddSpending.setOnClickListener {
            go(resolveAddButtonDestination())
        }

        return layout
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.standartd_list_menu_add -> {
                val dest = resolveAddButtonDestination()
                this.findNavController().navigate(dest)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun resolveAddButtonDestination() =
        ExpenditureListFragmentDirections.actionToExpenditureSpending(Action.ADD.name)

    protected open fun getAdapter() = ExpenditureAdapter(this)

    fun getDrawable(id: IconId?): Drawable {
        return this.getIconPack().icons[id?.value]?.drawable
            ?: AppCompatResources.getDrawable(requireContext(), R.drawable.ic_circle)!!
    }
}

class BudgetExpenditureListFragment: ExpenditureListFragment() {
    private val args: BudgetExpenditureListFragmentArgs by navArgs()

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

    override fun resolveAddButtonDestination(): NavDirections {
        return BudgetExpenditureListFragmentDirections
            .actionToBudgetExpenditure(Action.ADD.name, NEW_ITEM, args.budgetId)
    }

    override fun getAdapter() = BudgetExpenditureAdapter(this)
}
