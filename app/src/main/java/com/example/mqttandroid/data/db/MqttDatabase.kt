package com.example.mqttandroid.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mqttandroid.data.db.dao.BrokerConfigDao
import com.example.mqttandroid.data.db.dao.ReceivedMessageDao
import com.example.mqttandroid.data.db.dao.TopicSubscriptionDao
import com.example.mqttandroid.data.db.entity.BrokerConfigEntity
import com.example.mqttandroid.data.db.entity.ReceivedMessageEntity
import com.example.mqttandroid.data.db.entity.TopicSubscriptionEntity

@Database(
    entities = [
        BrokerConfigEntity::class,
        TopicSubscriptionEntity::class,
        ReceivedMessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MqttDatabase : RoomDatabase() {
    abstract fun brokerConfigDao(): BrokerConfigDao
    abstract fun topicSubscriptionDao(): TopicSubscriptionDao
    abstract fun receivedMessageDao(): ReceivedMessageDao
}
