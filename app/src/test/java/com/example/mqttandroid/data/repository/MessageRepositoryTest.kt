package com.example.mqttandroid.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mqttandroid.data.db.MqttDatabase
import com.example.mqttandroid.data.model.StoredMessage
import com.example.mqttandroid.data.model.TopicSubscription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MessageRepositoryTest {
    private lateinit var database: MqttDatabase
    private lateinit var topicRepository: TopicRepository
    private lateinit var messageRepository: MessageRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MqttDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        topicRepository = TopicRepository(
            topicSubscriptionDao = database.topicSubscriptionDao(),
            receivedMessageDao = database.receivedMessageDao()
        )
        messageRepository = MessageRepository(database.receivedMessageDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun clearHistoryRemovesOnlyMessagesForTargetTopic() = runTest {
        val firstTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/a", displayName = "Alerts A", qos = 0)
        )
        val secondTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/b", displayName = "Alerts B", qos = 1)
        )

        messageRepository.save(
            receivedMessage(
                topicId = firstTopicId,
                topic = "alerts/a",
                payload = "first",
                receivedAtEpochMillis = 1L
            )
        )
        messageRepository.save(
            receivedMessage(
                topicId = secondTopicId,
                topic = "alerts/b",
                payload = "second",
                receivedAtEpochMillis = 2L
            )
        )

        messageRepository.clearTopicHistory(firstTopicId)

        assertEquals(emptyList(), messageRepository.observeMessages(firstTopicId).first())
        assertEquals(
            listOf("second"),
            messageRepository.observeMessages(secondTopicId)
                .first()
                .map { it.payload.decodeToString() }
        )
    }

    @Test
    fun markTopicReadRemovesThatTopicFromUnreadCounts() = runTest {
        val firstTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/a", displayName = "Alerts A", qos = 0)
        )
        val secondTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/b", displayName = "Alerts B", qos = 0)
        )

        messageRepository.save(
            receivedMessage(
                topicId = firstTopicId,
                topic = "alerts/a",
                payload = "one",
                receivedAtEpochMillis = 1L
            )
        )
        messageRepository.save(
            receivedMessage(
                topicId = secondTopicId,
                topic = "alerts/b",
                payload = "two",
                receivedAtEpochMillis = 2L
            )
        )

        messageRepository.markTopicRead(firstTopicId)

        val unreadCounts = messageRepository.observeUnreadCountsByTopic().first()
        assertEquals(null, unreadCounts[firstTopicId])
        assertEquals(1, unreadCounts[secondTopicId])
    }

    private fun receivedMessage(
        topicId: Long,
        topic: String,
        payload: String,
        receivedAtEpochMillis: Long
    ) = StoredMessage(
        topicId = topicId,
        topic = topic,
        payload = payload.encodeToByteArray(),
        qos = 0,
        retained = false,
        receivedAtEpochMillis = receivedAtEpochMillis,
        isRead = false
    )
}
