package com.example.predasafe.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Handles creating the notification channel and posting local notifications.
 * Call createChannel() once at app startup (idempotent).
 */
object NotificationHelper {

    const val CHANNEL_ID   = "predasafe_alerts"
    const val CHANNEL_NAME = "PredaSafe Alerts"
    private var notificationId = 1000

    /** Creates the notification channel (required on Android 8+). Safe to call repeatedly. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wildlife motion and detection alerts from PredaSafe"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a local notification with the given title and body.
     * Does nothing if the POST_NOTIFICATIONS permission has not been granted (Android 13+).
     */
    fun showNotification(context: Context, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }
}
