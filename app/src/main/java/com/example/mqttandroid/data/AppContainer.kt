package com.example.mqttandroid.data

import android.content.Context
import androidx.room.Room
import com.example.mqttandroid.data.db.MqttDatabase
import com.example.mqttandroid.data.repository.BrokerRepository
import com.example.mqttandroid.data.repository.MessageRepository
import com.example.mqttandroid.data.repository.TopicRepository
import com.example.mqttandroid.domain.MqttConnectionVerifier
import com.example.mqttandroid.mqtt.PahoMqttClientGateway
import com.example.mqttandroid.setup.generateStartupClientId

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        MqttDatabase::class.java,
        "mqtt-android.db"
    ).build()

    val startupClientId = generateStartupClientId()
    val brokerRepository = BrokerRepository(database.brokerConfigDao())
    val topicRepository = TopicRepository(
        topicSubscriptionDao = database.topicSubscriptionDao(),
        receivedMessageDao = database.receivedMessageDao()
    )
    val messageRepository = MessageRepository(database.receivedMessageDao())
    val connectionVerifier = MqttConnectionVerifier(
        gatewayFactory = { PahoMqttClientGateway(appContext) }
    )
}
