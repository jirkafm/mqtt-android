package com.example.mqttandroid.domain

import com.example.mqttandroid.data.model.StoredMessage
import com.example.mqttandroid.data.model.TopicSubscription
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IncomingMessageProcessorTest {
    @Test
    fun incomingMessageIsStoredUnreadForMatchingTopic() = runTest {
        var storedMessage: StoredMessage? = null
        val processor = IncomingMessageProcessor(
            topicLookup = {
                TopicSubscription(
                    id = 5L,
                    topic = "alerts/door",
                    displayName = "Door",
                    qos = 1
                )
            },
            saveMessage = {
                storedMessage = it
                10L
            }
        )

        val saved = processor.process(
            topic = "alerts/door",
            payload = "opened".encodeToByteArray(),
            qos = 1,
            retained = false
        )

        assertEquals(10L, saved?.storedMessageId)
        assertNotNull(storedMessage)
        assertEquals(5L, storedMessage?.topicId)
        assertEquals(false, storedMessage?.isRead)
    }
}
