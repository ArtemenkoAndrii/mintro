package com.mas.mobile.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters

class DateTool {
    companion object {
        private const val TIME = "HH:mm"
        private const val DATE_TIME = "d.MM HH:mm"
        private const val DATE = "dd/MM/yyyy"
        private const val ABBREVIATED_DATE  = "dd MMM, yyyy"

        fun timeAsString(date: LocalDateTime): String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(TIME)
            return date.format(formatter)
        }

        fun dateTimeToString(date: LocalDateTime): String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME)
            return date.format(formatter)
        }

        fun stringToDateTime(date: String): LocalDateTime {
            val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
                .appendPattern(DATE_TIME)
                .parseDefaulting(ChronoField.YEAR, LocalDate.now().year.toLong())
                .toFormatter()
            return LocalDateTime.parse(date, formatter)
        }

        fun dateToString(date: LocalDate): String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE)
            return date.format(formatter)
        }

        fun dateToAbbreviatedString(date: LocalDate): String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(ABBREVIATED_DATE)
            return date.format(formatter)
        }

        fun stringToDate(date: String): LocalDate {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE)
            return LocalDate.parse(date, formatter)
        }

        fun localDateToLong(date: LocalDate): Long = date.toEpochDay()
        fun longToLocalDate(date: Long): LocalDate = LocalDate.ofEpochDay(date)
        fun localDateTimeToLong(date: LocalDateTime): Long = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun longToLocalDateTime(date: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault())

        fun formatToMonthWithYear(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        fun formatToWeekWithDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy"))
        fun formatToQuarterWithYear(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("QQQ yyyy"))
        fun formatToYear(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("yyyy"))
        fun formatToDayOfWeek(dayOfWeek: DayOfWeek): String =
            LocalDate.now().with(TemporalAdjusters.next(dayOfWeek)).format(DateTimeFormatter.ofPattern("EEEE"))
    }
}

fun LocalDate.toEpochMilli() =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
