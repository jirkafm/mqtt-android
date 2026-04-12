package com.example.mqttandroid.mqtt

data class MqttConnectionConfig(
    val serverUri: String,
    val clientId: String,
    val username: String? = null,
    val password: String? = null,
    val cleanSession: Boolean = true,
    val keepAliveSeconds: Int = 60,
    val autoReconnect: Boolean = true
)
