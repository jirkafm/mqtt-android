package com.example.mqttandroid.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topic_subscriptions",
    indices = [Index(value = ["topic"], unique = true)]
)
data class TopicSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val displayName: String,
    val qos: Int,
    val notificationsEnabled: Boolean = true,
    val subscriptionEnabled: Boolean = true,
    val lastError: String? = null
)
