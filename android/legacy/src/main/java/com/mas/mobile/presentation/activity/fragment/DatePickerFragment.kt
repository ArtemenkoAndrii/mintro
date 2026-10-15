package com.mas.mobile.presentation.activity.fragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.mas.mobile.util.toEpochMilli
import java.time.LocalDate

class DatePickerFragment(
    private var startDate: LocalDate,
    private var minDate: LocalDate?,
    private var maxDate: LocalDate?,
    private var listener: DatePickerDialog.OnDateSetListener
): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return DatePickerDialog(requireActivity(), listener, startDate.year, startDate.month.value-1, startDate.dayOfMonth).also {
            if (this.minDate != null) {
                it.datePicker.minDate = this.minDate!!.toEpochMilli()
            }

            if (this.maxDate != null) {
                it.datePicker.maxDate = this.maxDate!!.toEpochMilli()
            }
        }
    }
}