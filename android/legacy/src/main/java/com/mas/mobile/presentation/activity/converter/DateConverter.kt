package com.mas.mobile.presentation.activity.converter

import androidx.databinding.InverseMethod
import com.mas.mobile.util.DateTool
import java.time.LocalDate
import java.time.LocalDateTime

object DateConverter {
    @InverseMethod("stringToDate")
    @JvmStatic
    fun dateToString(value: LocalDate?) =
        value?.let { DateTool.dateToString(it) }

    @JvmStatic
    fun stringToDate(value: String?) =
        value?.let { DateTool.stringToDate(it) }

    @InverseMethod("stringToDateTime")
    @JvmStatic
    fun dateTimeToString(value: LocalDateTime?): String {
        return if (value == null) {
            ""
        } else {
            DateTool.dateTimeToString(value)
        }
    }

    @JvmStatic
    fun stringToDateTime(value: String): LocalDateTime {
        return DateTool.stringToDateTime(value)
    }

    @JvmStatic
    fun dateTimeToTimeAsString(value: LocalDateTime): String {
        return DateTool.timeAsString(value)
    }

    @JvmStatic
    fun dateTimeToAbbreviatedString(value: LocalDateTime): String {
        return DateTool.dateToAbbreviatedString(value.toLocalDate())
    }
}