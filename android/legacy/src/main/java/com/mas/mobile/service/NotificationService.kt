package com.mas.mobile.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mas.mobile.R
import javax.inject.Singleton

@Singleton
class NotificationService(
    private val context: Context,
    private val resourceService: ResourceService
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun sendUpdateAvailable() {
        val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.vending")
        }
        val pendingUpdateIntent: PendingIntent = PendingIntent.getActivity(context, 0, updateIntent, PendingIntent.FLAG_IMMUTABLE)

        sendNotification(
            resourceService.notificationNewVersionTitle(),
            resourceService.notificationNewVersionText(),
            pendingUpdateIntent
        )
    }

    private fun sendNotification(title: String,
                                 text: String,
                                 content: PendingIntent,
                                 vararg  actions: NotificationCompat.Action) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_expense)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(content)
            .setAutoCancel(true)

        actions.forEach {
            builder.addAction(it)
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(VERSION_UPDATE_ID, builder.build())
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance).apply {
                description = CHANNEL_ID
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "MasMoney"
        const val VERSION_UPDATE_ID  = 1
    }
}