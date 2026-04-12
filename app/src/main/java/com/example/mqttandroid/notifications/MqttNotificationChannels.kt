package com.example.mqttandroid.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.mqttandroid.R

object MqttNotificationChannels {
    const val SYNC_STATUS = "sync_status"
    const val INCOMING_MESSAGES = "incoming_messages"

    fun register(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SYNC_STATUS,
                context.getString(R.string.notification_channel_sync),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                INCOMING_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}
