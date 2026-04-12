package com.example.mqttandroid.mqtt

data class MqttIncomingMessage(
    val topic: String,
    val payload: ByteArray,
    val qos: Int,
    val retained: Boolean
)
