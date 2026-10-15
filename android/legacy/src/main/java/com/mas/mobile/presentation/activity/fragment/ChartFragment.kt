package com.mas.mobile.presentation.activity.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.databinding.ChartFragmentBinding
import com.mas.mobile.domain.analytics.OverspendingAlert
import com.mas.mobile.domain.analytics.Share
import com.mas.mobile.domain.analytics.TrendEntry
import com.mas.mobile.domain.analytics.Type
import com.mas.mobile.presentation.viewmodel.ChartViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency


class ChartFragment : CommonFragment() {
    private lateinit var binding: ChartFragmentBinding
    private val args: ChartFragmentArgs by navArgs()

    private val viewModel: ChartViewModel by lazyViewModel {
        this.requireContext().appComponent.chartViewModelModel().create(args.type)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val layout = inflater.inflate(R.layout.chart_fragment, container, false)
        binding = ChartFragmentBinding.bind(layout)

        with(binding) {
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.autocomplete_item,
                viewModel.availableFilterValues
            )
            filter.setAdapter(adapter)
            filter.setOnItemClickListener { parent, _, position, _ ->
                filter.clearFocus()
                viewModel.filter.value = parent.getItemAtPosition(position).toString()
            }

            when (args.type) {
                Type.AnalyticsTrends -> {
                    lineChart.visibility = View.VISIBLE
                    filterLayout.isHintEnabled = false
                    filter.hint = getResourceService().hintChartExpenditures()
                    filter.doAfterTextChanged {
                        if (filter.text.isNullOrEmpty()) {
                            viewModel.filter.value = null
                            filter.clearFocus()
                        }
                    }
                }
                Type.OverspendingAlerts -> {
                    barChart.visibility = View.VISIBLE
                    filterLayout.visibility = View.GONE
                }
                Type.ExpenditureDistribution -> {
                    if (viewModel.availableFilterValues.isNotEmpty()) {
                        filter.setText(viewModel.availableFilterValues[0], false)
                    }
                    pieChart.visibility = View.VISIBLE
                }
            }
        }
        viewModel.analyticsTrends.observe(viewLifecycleOwner) {
            buildLineChart(binding.lineChart, toLineData(it))
        }

        viewModel.overspendingAlerts.observe(viewLifecycleOwner) {
            buildBarChart(binding.barChart, toBarData(it))
        }

        viewModel.expenditureDistribution.observe(viewLifecycleOwner) {
            buildPieChart(binding.pieChart, toPieData(it))
        }

        return layout
    }

    private fun buildLineChart(chart: LineChart, chartData: ChartData<Entry>) {
        val planDataSet = LineDataSet(chartData.groups[0].list, chartData.groups[0].name).apply {
            color = Color.BLUE
            lineWidth = 2f
        }

        val factDataSet = LineDataSet(chartData.groups[1].list, chartData.groups[1].name).apply {
            color = Color.RED
            lineWidth = 2f
        }

        val lineData = LineData(planDataSet, factDataSet)

        with(chart) {
            clear()
            setNoDataText(getResourceService().messageAnalyticsNoBudgets())
            data = if (lineData.entryCount > 0) { lineData } else { null }

            xAxis.labelCount = chartData.labels.size
            xAxis.granularity = 1f
            xAxis.axisMinimum = 0f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(chartData.labels)
            xAxis.setCenterAxisLabels(false)

            isScaleXEnabled = true
            isScaleYEnabled = false

            description.isEnabled = false
            invalidate()
        }
    }

    private fun buildPieChart(chart: PieChart, chartData: ChartData<ChartPieEntry>) {
        val sum = chartData.groups[0].list.map { it.value }.sum()
        val legendEntries = buildLegendEntries(chartData.groups[0].list)
        val chartEntries = chartData.groups[0].list.map { PieEntry(it.value, it.label.crop() ) }

        val dataSet = PieDataSet(chartEntries, chartData.groups[0].name)
        dataSet.colors = COLORS
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                val percentage = value.getPercentageOf(sum)
                return if (percentage < LABEL_DISPLAY_THRESHOLD) {
                    ""
                } else {
                    pieEntry?.label ?: ""
                }
            }
        }
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 13f

        with(chart) {
            clear()
            setDrawEntryLabels(false)

            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(entry: Entry?, highlight: Highlight?) {
                    entry?.let {
                        val index = chartEntries.indexOf(entry)
                        centerText = if (index >= 0) {
                            val data = chartData.groups[0].list[index]
                            String.format(CENTER_TEXT_MASK, data.label, data.value, data.currency.symbol)
                        } else {
                            ""
                        }
                    }
                }

                override fun onNothingSelected() {
                    centerText = ""
                }
            })

            with(legend) {
                setCustom(legendEntries)
                setDrawInside(false)
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                textSize = 12f
            }
            description.isEnabled = false
            data = PieData(dataSet)
            invalidate()
        }
    }

    private fun buildBarChart(chart: HorizontalBarChart, chartData: ChartData<BarEntry>) {
        val dataSet = BarDataSet(chartData.groups[0].list, chartData.groups[0].name).also {
            it.setDrawValues(true)
            it.color = Color.RED
            it.valueTextSize = 10f
            it.valueFormatter = PercentFormatter()
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        with(chart) {
            clear()
            setNoDataText(getResourceService().messageAnalyticsNoAlerts())
            data = if (barData.entryCount > 0) { barData } else { null }
            description.isEnabled = false
            legend.isEnabled = false
            isScaleXEnabled = false
            isScaleYEnabled = false
        }

        with(chart.xAxis) {
            setDrawGridLines(true)
            axisMinimum = -0.45f
            axisMaximum = chartData.labels.size -0.55f
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = chartData.labels.size
            valueFormatter = IndexAxisValueFormatter(chartData.labels)
        }

        with(chart.axisLeft) {
            axisMinimum = 0f
            isEnabled = true
        }

        with(chart.axisRight) {
            isEnabled = false
        }

        chart.invalidate()
    }

    private fun Float.getPercentageOf(amount: Float) =
        if (amount > 0) {
            BigDecimal((this / amount * 100).toString())
                .setScale(2, RoundingMode.HALF_EVEN)
                .toFloat()
        } else {
            0f
        }

    private fun buildLegendEntries(entries: List<PieEntry>): List<LegendEntry> {
        val sum = entries.map { it.value }.sum()
        return entries.mapIndexed { index, pieEntry ->
            val percentage = pieEntry.value.getPercentageOf(sum)
            LegendEntry(
                String.format(LEGEND_MASK, pieEntry.label, percentage),
                Legend.LegendForm.SQUARE,
                10f,
                5f,
                null,
                COLORS[index % COLORS.size]
            )
        }
    }

    private fun String.crop() =
        if (this.length > LABEL_MAX_LENGTH) {
            this.substring(0, LABEL_MAX_LENGTH - LABEL_CONFORMATION.length - 1) + LABEL_CONFORMATION
        } else {
            this
        }

    private fun toLineData(data: List<TrendEntry>): ChartData<Entry> =
        ChartData(
            labels = data.map { it.name.crop() },
            groups = arrayOf(
                ChartData.Group(
                    getResourceService().labelAnalyticsPlan(),
                    data.mapIndexed { index, trendEntry -> Entry(index.toFloat(), trendEntry.plan.toFloat()) }
                ),
                ChartData.Group(
                    getResourceService().labelAnalyticsFact(),
                    data.mapIndexed { index, trendEntry -> Entry(index.toFloat(), trendEntry.fact.toFloat()) }
                )
            )
        )

    private fun toBarData(data: List<OverspendingAlert>): ChartData<BarEntry> =
        ChartData(
            labels = data.map { it.expenditureName.crop() },
            groups = arrayOf(
                ChartData.Group(
                    "percentage",
                    data.mapIndexed { index, oa -> BarEntry(index.toFloat(), oa.overspending.value.toFloat()) }
                )
            )
        )

    private fun toPieData(data: List<Share>): ChartData<ChartPieEntry> =
        ChartData(
            labels = emptyList(),
            groups = arrayOf(
                ChartData.Group(
                    "percentage",
                    data.map { ChartPieEntry(it.fact.toFloat(), it.name, it.currency) }.toList()
                )
            )
        )

    class ChartData<T>(
        val labels: List<String>,
        val groups: Array<Group<T>>
    ) {
        data class Group<T>(
            val name: String,
            val list: List<T>
        )
    }

    class ChartPieEntry(value: Float, label: String, val currency: Currency) : PieEntry(value, label)

    companion object {
        const val LABEL_DISPLAY_THRESHOLD = 3
        const val LABEL_MAX_LENGTH = 14
        const val LABEL_CONFORMATION = ".."
        const val CENTER_TEXT_MASK = "%s\n%.2f%s"
        const val LEGEND_MASK = "%s - %.1f%%"

        val COLORS = listOf(
            Color.rgb(255, 0, 0), // Red
            Color.rgb(0, 255, 0), // Green
            Color.rgb(0, 0, 255), // Blue
            Color.rgb(255, 255, 0), // Yellow
            Color.rgb(0, 255, 255), // Cyan
            Color.rgb(255, 0, 255), // Magenta
            Color.rgb(192, 192, 192), // Light Gray
            Color.rgb(128, 128, 128), // Gray
            Color.rgb(128, 0, 0), // Maroon
            Color.rgb(128, 128, 0), // Olive
            Color.rgb(0, 128, 0), // Dark Green
            Color.rgb(128, 0, 128), // Purple
            Color.rgb(0, 128, 128), // Teal
            Color.rgb(0, 0, 128), // Navy
            Color.rgb(255, 165, 0), // Orange
            Color.rgb(255, 69, 0), // Red Orange
            Color.rgb(218, 112, 214), // Orchid
            Color.rgb(238, 130, 238), // Violet
            Color.rgb(75, 0, 130), // Indigo
            Color.rgb(240, 230, 140), // Khaki
            Color.rgb(144, 238, 144)  // Light Green
        )
    }
}
