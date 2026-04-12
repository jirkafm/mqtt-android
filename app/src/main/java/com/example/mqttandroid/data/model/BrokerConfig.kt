package com.example.mqttandroid.data.model

data class BrokerConfig(
    val serverUri: String,
    val clientId: String,
    val username: String? = null,
    val password: String? = null,
    val tlsEnabled: Boolean = false,
    val cleanSession: Boolean = true,
    val keepAliveSeconds: Int = 60,
    val autoReconnect: Boolean = true,
    val syncEnabled: Boolean = false,
    val updatedAtEpochMillis: Long
)
