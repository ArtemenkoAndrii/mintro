package com.mas.mobile.repository.db.config.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mas.mobile.domain.message.Message
import com.mas.mobile.util.DateTool
import java.time.LocalDate
import java.time.LocalDateTime

const val TRUE: Byte = 1
const val FALSE: Byte = 0

class SQLiteTypeConverter {

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let { DateTool.longToLocalDateTime(value) }
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? {
        return value?.let { DateTool.localDateTimeToLong(value) }
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { DateTool.longToLocalDate(value) }
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.let { DateTool.localDateToLong(value) }
    }

    @TypeConverter
    fun toBoolean(value: Byte?): Boolean? {
        return value?.equals(TRUE)
    }

    @TypeConverter
    fun fromBoolean(value: Boolean?): Byte? {
        return value?.let{ if (it) TRUE else FALSE }
    }

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> {
        return Gson().fromJson(value,  object : TypeToken<Map<String, String>>() {}.type)
    }

    @TypeConverter
    fun mapToString(value: Map<String, String>?): String {
        return Gson().toJson(value) ?: ""
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value == "") return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}