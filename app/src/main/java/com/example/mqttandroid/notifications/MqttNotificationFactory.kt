package com.example.mqttandroid.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mqttandroid.R
import com.example.mqttandroid.mqtt.MqttConnectionState

class MqttNotificationDecision {
    fun shouldNotify(notificationsEnabled: Boolean): Boolean = notificationsEnabled
}

class MqttNotificationFactory(
    private val context: Context,
    private val decision: MqttNotificationDecision = MqttNotificationDecision()
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun createForegroundNotification(state: MqttConnectionState): Notification {
        val title = when (state) {
            is MqttConnectionState.Connected -> context.getString(R.string.sync_connected)
            is MqttConnectionState.Connecting -> context.getString(R.string.sync_connecting)
            is MqttConnectionState.Disconnected -> context.getString(R.string.sync_disconnected)
            is MqttConnectionState.Error -> context.getString(R.string.sync_error)
        }
        val text = when (state) {
            is MqttConnectionState.Connected -> context.getString(R.string.sync_connected_description)
            is MqttConnectionState.Connecting -> context.getString(R.string.sync_connecting_description)
            is MqttConnectionState.Disconnected -> context.getString(R.string.sync_disconnected_description)
            is MqttConnectionState.Error -> state.message
        }

        return NotificationCompat.Builder(context, MqttNotificationChannels.SYNC_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    fun notifyIncomingMessage(
        topicId: Long,
        topicLabel: String,
        payloadPreview: String,
        notificationsEnabled: Boolean
    ) {
        if (!decision.shouldNotify(notificationsEnabled)) return
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, MqttNotificationChannels.INCOMING_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_new_message_title))
            .setContentText("$topicLabel: $payloadPreview")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$topicLabel: $payloadPreview"))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(topicId.toInt(), notification)
    }

    fun updateForeground(service: android.app.Service, state: MqttConnectionState) {
        val notification = createForegroundNotification(state)
        service.startForeground(1001, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
