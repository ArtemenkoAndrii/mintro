package com.mas.mobile.presentation.activity.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.mas.mobile.R
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.viewmodel.ItemViewModel
import com.mas.mobile.presentation.viewmodel.validator.Action
import com.mas.mobile.util.DateTool
import com.mynameismidori.currencypicker.CurrencyPicker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

abstract class ItemFragment<T: ItemViewModel<*>>: CommonFragment() {
    protected lateinit var action: Action
    abstract val viewModel: T

    protected fun addBudgetToTitle(budgetId: Int) {
        val bar = (this.requireActivity() as MainActivity).supportActionBar
        if (bar != null) {
            bar.title = "[${getBudgetName(budgetId)}] ${bar.title}"
        }
    }

    protected fun showDateDialog(startDate: LocalDate = LocalDate.now(),
                                 minDate: LocalDate? = null,
                                 maxDate: LocalDate? = null,
                                 callback: (LocalDate) -> Unit) {
        DatePickerFragment(startDate, minDate, maxDate) { _, year, month, day ->
            callback(LocalDate.of(year, month + 1, day))
        }.show(requireActivity().supportFragmentManager, "dateTimePicker")
    }

    protected fun showDateTimeDialog(editor: TextInputEditText) {
        val current = try {
            DateTool.stringToDateTime(editor.text.toString())
        } catch (e: Throwable) {
            LocalDateTime.now()
        }

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val dateTime = LocalDateTime.of(year, month + 1, day, hour, minute)
                val result = DateTool.dateTimeToString(dateTime)
                editor.setText(result)
            }, current.hour, current.minute, true).show()
        }, current.year, current.month.value-1, current.dayOfMonth).show()
    }

    private fun getBudgetName(budgetId: Int): String {
        return "FIX ME"
    }

    fun saveAndClose(viewModel: T) {
        if (viewModel.save()) {
            findNavController().popBackStack()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.standard_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.standard_menu_edit -> {
                onEdit()
                action = Action.EDIT
                true
            }
            R.id.standard_menu_remove -> {
                onRemove()
                action = Action.REMOVE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    open fun onEdit() {
        viewModel.enableEditing()
    }

    open fun onRemove() {
        showConfirmationDialog(getLabel()) {
            viewModel.remove()
            this.findNavController().popBackStack()
        }
    }

    private fun getLabel() = context?.getString(R.string.dialog_confirmation_remove) ?: ""
}
