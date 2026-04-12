package com.example.mqttandroid.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_messages",
    foreignKeys = [
        ForeignKey(
            entity = TopicSubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["topicId", "isRead"]),
        Index(value = ["receivedAtEpochMillis"])
    ]
)
data class ReceivedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val topic: String,
    val payload: ByteArray,
    val qos: Int,
    val retained: Boolean,
    val receivedAtEpochMillis: Long,
    val isRead: Boolean
)
