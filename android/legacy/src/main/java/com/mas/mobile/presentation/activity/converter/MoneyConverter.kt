package com.mas.mobile.presentation.activity.converter

import androidx.databinding.InverseMethod

object MoneyConverter {
    @InverseMethod("stringToDouble")
    @JvmStatic
    fun doubleToString(value: Double): String {
        return String.format("%.2f", value)
    }

    @JvmStatic
    fun stringToDouble(value: String): Double {
        return value.replace(",", ".").toDouble()
    }
}