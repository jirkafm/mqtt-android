package com.example.mqttandroid.domain

import com.example.mqttandroid.data.model.BrokerConfig
import com.example.mqttandroid.data.model.TopicSubscription
import com.example.mqttandroid.mqtt.MqttClientGateway
import com.example.mqttandroid.mqtt.MqttConnectionConfig

class MqttConnectionVerifier(
    private val gatewayFactory: () -> MqttClientGateway
) {
    suspend fun verify(
        config: BrokerConfig,
        topics: List<TopicSubscription>
    ) {
        val gateway = gatewayFactory()
        try {
            gateway.connect(config.toConnectionConfig())
            topics
                .filter { it.subscriptionEnabled }
                .forEach { topic ->
                    gateway.subscribe(topic.topic, topic.qos)
                }
        } finally {
            gateway.disconnect()
        }
    }
}

private fun BrokerConfig.toConnectionConfig() = MqttConnectionConfig(
    serverUri = serverUri,
    clientId = clientId,
    username = username,
    password = password,
    cleanSession = cleanSession,
    keepAliveSeconds = keepAliveSeconds,
    autoReconnect = autoReconnect
)
