package com.mas.mobile.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.mas.mobile.R
import com.mas.mobile.presentation.activity.MainActivity
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    private val defaultMessage = context.getString(R.string.error_unexpected)

    fun handleAndNotify(error: Throwable,  message: String = defaultMessage) {
        Log.e(MainActivity::class.java.name, message, error)
        toast(message)
    }

    fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}