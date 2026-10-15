package com.mas.mobile.presentation.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mas.mobile.R
import com.mas.mobile.databinding.AnalyticsFragmentBinding
import com.mas.mobile.domain.analytics.Type


class AnalyticsFragment : CommonFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.analytics_fragment, container, false)

        with(AnalyticsFragmentBinding.bind(layout)) {
            lifecycleOwner = this@AnalyticsFragment

            analyticsTrends.setOnClickListener {
                go(AnalyticsFragmentDirections.actionToChart(Type.AnalyticsTrends))
            }

            overspendingAlerts.setOnClickListener {
                go(AnalyticsFragmentDirections.actionToChart(Type.OverspendingAlerts))
            }

            expenditureDistribution.setOnClickListener {
                go(AnalyticsFragmentDirections.actionToChart(Type.ExpenditureDistribution))
            }
        }

        return layout
    }
}

