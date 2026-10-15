package com.mas.mobile.service

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionService @Inject constructor(
    private val context: Context
) {
    fun isSMSAllowed(): Boolean =
        when(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }

    fun isInternetAllowed(): Boolean =
        when(ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET)) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }

    fun isNotificationsAllowed(): Boolean {
        val component = ComponentName(context, NotificationListener::class.java)
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.isNotificationListenerAccessGranted(component)
        } else {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(component.flattenToString())
        }
    }
}