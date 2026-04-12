package com.example.mqttandroid.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mqttandroid.data.db.entity.TopicSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicSubscriptionDao {
    @Query("SELECT * FROM topic_subscriptions ORDER BY displayName COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<TopicSubscriptionEntity>>

    @Query("SELECT * FROM topic_subscriptions ORDER BY displayName COLLATE NOCASE ASC, id ASC")
    suspend fun getAll(): List<TopicSubscriptionEntity>

    @Query("SELECT * FROM topic_subscriptions WHERE id = :topicId LIMIT 1")
    suspend fun getById(topicId: Long): TopicSubscriptionEntity?

    @Query("SELECT * FROM topic_subscriptions WHERE topic = :topic LIMIT 1")
    suspend fun getByTopic(topic: String): TopicSubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(topic: TopicSubscriptionEntity): Long

    @Update
    suspend fun update(topic: TopicSubscriptionEntity)

    @Transaction
    suspend fun upsertPreservingIdentity(topic: TopicSubscriptionEntity): Long {
        val insertedId = insert(topic)
        if (insertedId != -1L) {
            return insertedId
        }

        val existingId = getByTopic(topic.topic)?.id
            ?: error("Topic disappeared during upsert: ${topic.topic}")

        update(topic.copy(id = existingId))
        return existingId
    }

    @Query("DELETE FROM topic_subscriptions WHERE id = :topicId")
    suspend fun deleteById(topicId: Long)

    @Query("UPDATE topic_subscriptions SET subscriptionEnabled = :enabled WHERE id = :topicId")
    suspend fun updateSubscriptionEnabled(topicId: Long, enabled: Boolean)

    @Query("UPDATE topic_subscriptions SET notificationsEnabled = :enabled WHERE id = :topicId")
    suspend fun updateNotificationsEnabled(topicId: Long, enabled: Boolean)

    @Query("UPDATE topic_subscriptions SET lastError = :error WHERE id = :topicId")
    suspend fun updateLastError(topicId: Long, error: String?)
}
