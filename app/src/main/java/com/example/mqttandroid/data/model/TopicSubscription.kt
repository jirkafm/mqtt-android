package com.example.mqttandroid.data.model

data class TopicSubscription(
    val id: Long = 0,
    val topic: String,
    val displayName: String,
    val qos: Int,
    val notificationsEnabled: Boolean = true,
    val subscriptionEnabled: Boolean = true,
    val lastError: String? = null
)
