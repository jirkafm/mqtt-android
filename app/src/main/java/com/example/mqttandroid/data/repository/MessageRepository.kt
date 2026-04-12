package com.example.mqttandroid.data.repository

import com.example.mqttandroid.data.db.dao.ReceivedMessageDao
import com.example.mqttandroid.data.db.entity.ReceivedMessageEntity
import com.example.mqttandroid.data.model.StoredMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(
    private val receivedMessageDao: ReceivedMessageDao
) {
    fun observeMessages(topicId: Long): Flow<List<StoredMessage>> =
        receivedMessageDao.observeTopicMessages(topicId)
            .map { messages -> messages.map { it.toModel() } }

    fun observeUnreadCountsByTopic(): Flow<Map<Long, Int>> =
        receivedMessageDao.observeUnreadCountsByTopic()
            .map { counts ->
                counts.associate { count -> count.topicId to count.unreadCount }
            }

    suspend fun save(message: StoredMessage): Long =
        receivedMessageDao.insert(message.toEntity())

    suspend fun clearTopicHistory(topicId: Long) =
        receivedMessageDao.clearTopicHistory(topicId)

    suspend fun markTopicRead(topicId: Long) =
        receivedMessageDao.markTopicRead(topicId)

    suspend fun markMessageRead(messageId: Long) =
        receivedMessageDao.markMessageRead(messageId)
}

private fun ReceivedMessageEntity.toModel() = StoredMessage(
    id = id,
    topicId = topicId,
    topic = topic,
    payload = payload,
    qos = qos,
    retained = retained,
    receivedAtEpochMillis = receivedAtEpochMillis,
    isRead = isRead
)

private fun StoredMessage.toEntity() = ReceivedMessageEntity(
    id = id,
    topicId = topicId,
    topic = topic,
    payload = payload,
    qos = qos,
    retained = retained,
    receivedAtEpochMillis = receivedAtEpochMillis,
    isRead = isRead
)
