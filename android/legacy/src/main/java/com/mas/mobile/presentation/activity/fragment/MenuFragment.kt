package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import com.mas.mobile.R
import com.mas.mobile.databinding.MenuFragmentBinding
import com.mas.mobile.domain.budget.Budget


class MenuFragment : CommonFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.menu_fragment, container, false)

        with(MenuFragmentBinding.bind(layout)) {
            menuBudgetsLayout.navigateOnClick(MenuFragmentDirections.actionToBudgets())
            menuAnalyticsLayout.navigateOnClick(MenuFragmentDirections.actionToAnalytics())
            menuSettingsLayout.navigateOnClick(MenuFragmentDirections.actionToSettings())
            menuMessageOriginsLayout.navigateOnClick(MenuFragmentDirections.actionToMessageTemplates())
            menuCategoriesLayout.navigateOnClick(MenuFragmentDirections.actionToCategories())
            menuScheduledLayout.navigateOnClick(MenuFragmentDirections.actionToSpendingScheduled(
                Budget.SCHEDULED_BUDGET_ID
            ))
        }

        return layout
    }

    private fun View.navigateOnClick(direction: NavDirections) {
        this.setOnClickListener {
            go(direction)
        }
    }
}
