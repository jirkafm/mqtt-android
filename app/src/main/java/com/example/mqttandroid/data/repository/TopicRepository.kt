package com.example.mqttandroid.data.repository

import com.example.mqttandroid.data.db.dao.ReceivedMessageDao
import com.example.mqttandroid.data.db.dao.TopicSubscriptionDao
import com.example.mqttandroid.data.db.entity.TopicSubscriptionEntity
import com.example.mqttandroid.data.model.TopicSubscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.eclipse.paho.client.mqttv3.MqttTopic

class TopicRepository(
    private val topicSubscriptionDao: TopicSubscriptionDao,
    private val receivedMessageDao: ReceivedMessageDao
) {
    data class TopicSummary(
        val id: Long,
        val topic: String,
        val displayName: String,
        val qos: Int,
        val notificationsEnabled: Boolean,
        val subscriptionEnabled: Boolean,
        val unreadCount: Int
        )

    fun observeTopics(): Flow<List<TopicSubscription>> =
        topicSubscriptionDao.observeAll()
            .map { topics -> topics.map { it.toModel() } }

    fun observeTopicSummaries(): Flow<List<TopicSummary>> =
        combine(
            topicSubscriptionDao.observeAll(),
            receivedMessageDao.observeUnreadCountsByTopic()
        ) { topics, unreadCounts ->
            val unreadByTopicId = unreadCounts.associate { count ->
                count.topicId to count.unreadCount
            }

            topics.map { topic ->
                TopicSummary(
                    id = topic.id,
                    topic = topic.topic,
                    displayName = topic.displayName,
                    qos = topic.qos,
                    notificationsEnabled = topic.notificationsEnabled,
                    subscriptionEnabled = topic.subscriptionEnabled,
                    unreadCount = unreadByTopicId[topic.id] ?: 0
                )
            }
        }

    suspend fun getTopics(): List<TopicSubscription> =
        topicSubscriptionDao.getAll().map { it.toModel() }

    suspend fun saveTopic(topic: TopicSubscription): Long =
        topicSubscriptionDao.upsertPreservingIdentity(topic.toEntity())

    suspend fun deleteTopic(topicId: Long) =
        topicSubscriptionDao.deleteById(topicId)

    suspend fun findMatchingTopic(incomingTopic: String): TopicSubscription? =
        topicSubscriptionDao.getAll()
            .map { it.toModel() }
            .firstOrNull { savedTopic -> MqttTopic.isMatched(savedTopic.topic, incomingTopic) }

    suspend fun setSubscriptionEnabled(topicId: Long, enabled: Boolean) =
        topicSubscriptionDao.updateSubscriptionEnabled(topicId, enabled)

    suspend fun setNotificationsEnabled(topicId: Long, enabled: Boolean) =
        topicSubscriptionDao.updateNotificationsEnabled(topicId, enabled)

    suspend fun setLastError(topicId: Long, error: String?) =
        topicSubscriptionDao.updateLastError(topicId, error)
}

private fun TopicSubscriptionEntity.toModel() = TopicSubscription(
    id = id,
    topic = topic,
    displayName = displayName,
    qos = qos,
    notificationsEnabled = notificationsEnabled,
    subscriptionEnabled = subscriptionEnabled,
    lastError = lastError
)

private fun TopicSubscription.toEntity() = TopicSubscriptionEntity(
    id = id,
    topic = topic,
    displayName = displayName,
    qos = qos,
    notificationsEnabled = notificationsEnabled,
    subscriptionEnabled = subscriptionEnabled,
    lastError = lastError
)
