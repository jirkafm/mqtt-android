package com.example.mqttandroid.data.model

data class StoredMessage(
    val id: Long = 0,
    val topicId: Long,
    val topic: String,
    val payload: ByteArray,
    val qos: Int,
    val retained: Boolean,
    val receivedAtEpochMillis: Long,
    val isRead: Boolean
)
