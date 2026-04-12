package com.example.mqttandroid.domain

import com.example.mqttandroid.data.model.StoredMessage
import com.example.mqttandroid.data.model.TopicSubscription

class IncomingMessageProcessor(
    private val topicLookup: suspend (String) -> TopicSubscription?,
    private val saveMessage: suspend (StoredMessage) -> Long
) {
    data class ProcessedIncomingMessage(
        val storedMessageId: Long,
        val topic: TopicSubscription
    )

    suspend fun process(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean
    ): ProcessedIncomingMessage? {
        val matchedTopic = topicLookup(topic) ?: return null
        val storedMessageId = saveMessage(
            StoredMessage(
                topicId = matchedTopic.id,
                topic = topic,
                payload = payload,
                qos = qos,
                retained = retained,
                receivedAtEpochMillis = System.currentTimeMillis(),
                isRead = false
            )
        )
        return ProcessedIncomingMessage(
            storedMessageId = storedMessageId,
            topic = matchedTopic
        )
    }
}
