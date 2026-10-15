package com.mas.mobile.presentation.activity.binding

import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.maltaisn.icondialog.data.Icon
import com.mas.mobile.R
import com.mas.mobile.domain.budget.Budget
import com.mas.mobile.domain.budget.Expenditure
import com.mas.mobile.domain.budget.Recurrence
import com.mas.mobile.domain.budget.Spending
import com.mas.mobile.presentation.activity.converter.MoneyConverter
import com.mas.mobile.util.DateTool
import kotlin.math.absoluteValue


// Populates text input on model changed
@BindingAdapter("android:text")
fun TextInputEditText.bindEditText(value: Double) {
    val editorValue = this.text.toString()
    if (this.hasFocus()) {
        // Forbid more then two numbers after the dot
        val split = editorValue.split(".")
        if (split.size > 1 && split[1].length > 2) {
            val new = editorValue.substring(0, split[0].length+3)
            this.setText(new)
            this.setSelection(new.length)
        }
    } else {
        // Populate initial value
        val modelValue = MoneyConverter.doubleToString(value)
        if (editorValue != modelValue) {
            this.setText(modelValue)
        }
    }
}

// Populates model on text changed
@InverseBindingAdapter(attribute = "android:text")
fun TextInputEditText.bindInverseEditText(): Double {
    val textValue = this.text.toString()
    if (textValue.isEmpty()) return 0.0

    return MoneyConverter.stringToDouble(this.text.toString())
}

// Populates input on focus changed
@BindingAdapter("app:onFocusLost")
fun TextInputEditText.onFocusLost(value: Double) {
    setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            // Format the input if the focus is lost
            this.setText(MoneyConverter.doubleToString(value))
        } else {
            // Clear the input for the new value
            if (value == 0.0) { this.setText("") }
        }
    }
}

@BindingAdapter("app:progressColor")
fun LinearProgressIndicator.onFocusLost(value: Int?) {
    if (value != null){
        this.setIndicatorColor(this.context.getColor(value))
    }
}

@BindingAdapter("spannableText")
fun setSpannableText(view: TextView, value: SpannableString?) {
    if (view.text != value) {
        view.text = value
    }
}

@InverseBindingAdapter(attribute = "spannableText", event = "spannableTextAttrChanged")
fun getSpannableText(view: TextView): SpannableString {
    return SpannableString(view.text)
}

@BindingAdapter("spannableTextAttrChanged")
fun setSpannableTextListener(view: TextView, listener: InverseBindingListener?) {
    if (listener != null) {
        view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                listener.onChange()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}

@BindingAdapter("app:icon")
fun setIcon(view: ImageView, icon: Icon?) {
    if (icon != null) {
        view.setImageDrawable(icon.drawable)
    } else {
        view.setImageResource(R.drawable.ic_trash_bin_24)
    }
}

@BindingAdapter("expenditureProgress")
fun setExpenditureProgress(textView: TextView, expenditure: Expenditure) {
    textView.text = if (expenditure.plan.absoluteValue < 0.001) {
        textView.context.getString(R.string.formatter_unplanned)
    } else {
        val format = textView.context.getString(R.string.formatter_percent_of)
        String.format(format, expenditure.progress, expenditure.plan)
    }
}

@BindingAdapter("budgetProgress")
fun setBudgetProgress(textView: TextView, budget: Budget) {
    textView.text = if (budget.plan.absoluteValue < 0.001) {
        textView.context.getString(R.string.formatter_unplanned)
    } else {
        val format = textView.context.getString(R.string.formatter_percent_of)
        String.format(format, budget.getProgress(), budget.plan)
    }
}

@BindingAdapter("recurrenceText")
fun setRecurrenceText(textView: TextView, spending: Spending) {
    val context = textView.context
    val scheduled = spending.scheduledDate?.let { DateTool.dateTimeToString(it) }
    val recurrence = when(spending.recurrence) {
            Recurrence.Never -> context.getString(R.string.constant_recurrence_never)
            Recurrence.Daily -> context.getString(R.string.constant_recurrence_daily)
            Recurrence.Weekly -> context.getString(R.string.constant_recurrence_weekly)
            Recurrence.Monthly -> context.getString(R.string.constant_recurrence_monthly)
            Recurrence.Quarterly -> context.getString(R.string.constant_recurrence_quarterly)
        }

    textView.text = if (scheduled == null) {
        recurrence
    } else {
        val formatter = textView.context.getString(R.string.formatter_next_recurrences)
        String.format(formatter, recurrence, scheduled)
    }
}