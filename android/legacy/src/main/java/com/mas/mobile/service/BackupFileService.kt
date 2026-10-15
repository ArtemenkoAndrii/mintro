package com.mas.mobile.service

import android.content.Context
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupFileService @Inject constructor(
    private val context: Context
) {
    fun write(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(content.toByteArray())
        } ?: throw IllegalStateException("Unable to open $uri for writing")
    }

    fun read(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use {
            it.reader().readText()
        } ?: throw IllegalStateException("Unable to open $uri for reading")
}
