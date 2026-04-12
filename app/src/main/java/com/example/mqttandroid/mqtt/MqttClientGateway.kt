package com.example.mqttandroid.mqtt

import kotlinx.coroutines.flow.StateFlow

interface MqttClientGateway {
    val connectionState: StateFlow<MqttConnectionState>

    suspend fun connect(config: MqttConnectionConfig)
    suspend fun disconnect()
    suspend fun subscribe(topic: String, qos: Int)
    suspend fun unsubscribe(topic: String)
    fun setListener(listener: suspend (MqttIncomingMessage) -> Unit)
}
