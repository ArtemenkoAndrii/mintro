package com.mas.mobile.domain.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.time.LocalDate
import java.time.LocalDateTime

object BackupJson {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ -> LocalDate.parse(json.asString) })
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ -> LocalDateTime.parse(json.asString) })
        .create()

    fun toJson(file: BackupFile): String = gson.toJson(file)

    fun fromJson(json: String): BackupFile = gson.fromJson(json, BackupFile::class.java)
}
