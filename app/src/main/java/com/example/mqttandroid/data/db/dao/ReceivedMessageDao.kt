package com.example.mqttandroid.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mqttandroid.data.db.entity.ReceivedMessageEntity
import kotlinx.coroutines.flow.Flow

data class TopicUnreadCount(
    @ColumnInfo(name = "topicId") val topicId: Long,
    @ColumnInfo(name = "unreadCount") val unreadCount: Int
)

@Dao
interface ReceivedMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ReceivedMessageEntity): Long

    @Query("SELECT * FROM received_messages WHERE topicId = :topicId ORDER BY receivedAtEpochMillis DESC, id DESC")
    fun observeTopicMessages(topicId: Long): Flow<List<ReceivedMessageEntity>>

    @Query(
        """
        SELECT topicId, COUNT(*) AS unreadCount
        FROM received_messages
        WHERE isRead = 0
        GROUP BY topicId
        """
    )
    fun observeUnreadCountsByTopic(): Flow<List<TopicUnreadCount>>

    @Query("DELETE FROM received_messages WHERE topicId = :topicId")
    suspend fun clearTopicHistory(topicId: Long)

    @Query("UPDATE received_messages SET isRead = 1 WHERE topicId = :topicId AND isRead = 0")
    suspend fun markTopicRead(topicId: Long)

    @Query("UPDATE received_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markMessageRead(messageId: Long)
}
