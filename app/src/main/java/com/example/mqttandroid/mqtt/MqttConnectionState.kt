package com.example.mqttandroid.mqtt

sealed interface MqttConnectionState {
    data object Disconnected : MqttConnectionState
    data object Connecting : MqttConnectionState
    data class Connected(val serverUri: String) : MqttConnectionState
    data class Error(val message: String) : MqttConnectionState
}
