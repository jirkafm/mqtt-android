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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TopicRepositoryTest {
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
    fun unreadCountReflectsUnreadMessagesForEachTopic() = runTest {
        val firstTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/a", displayName = "A", qos = 0)
        )
        val secondTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/b", displayName = "B", qos = 0)
        )

        messageRepository.save(
            receivedMessage(firstTopicId, "alerts/a", "one", 1L)
        )
        messageRepository.save(
            receivedMessage(firstTopicId, "alerts/a", "two", 2L)
        )
        messageRepository.save(
            receivedMessage(secondTopicId, "alerts/b", "three", 3L)
        )

        val summaries = topicRepository.observeTopicSummaries().first()

        assertEquals(2, summaries.first { it.id == firstTopicId }.unreadCount)
        assertEquals(1, summaries.first { it.id == secondTopicId }.unreadCount)
    }

    @Test
    fun savingExistingTopicPreservesTopicIdAndMessageHistory() = runTest {
        val originalTopicId = topicRepository.saveTopic(
            TopicSubscription(topic = "alerts/door", displayName = "Door", qos = 0)
        )

        messageRepository.save(
            receivedMessage(originalTopicId, "alerts/door", "opened", 1L)
        )

        val updatedTopicId = topicRepository.saveTopic(
            TopicSubscription(
                topic = "alerts/door",
                displayName = "Front Door",
                qos = 1,
                notificationsEnabled = false,
                subscriptionEnabled = true
            )
        )

        val topics = topicRepository.getTopics()
        val messages = messageRepository.observeMessages(originalTopicId).first()

        assertEquals(originalTopicId, updatedTopicId)
        assertEquals(1, topics.size)
        assertEquals("Front Door", topics.single().displayName)
        assertEquals(1, topics.single().qos)
        assertEquals(false, topics.single().notificationsEnabled)
        assertEquals(listOf("opened"), messages.map { it.payload.decodeToString() })
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
