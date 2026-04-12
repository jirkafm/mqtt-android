package com.example.mqttandroid.domain

import com.example.mqttandroid.data.model.BrokerConfig
import com.example.mqttandroid.data.model.TopicSubscription
import com.example.mqttandroid.mqtt.MqttClientGateway
import com.example.mqttandroid.mqtt.MqttConnectionConfig
import com.example.mqttandroid.mqtt.MqttConnectionState
import com.example.mqttandroid.mqtt.MqttIncomingMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MqttConnectionVerifierTest {
    @Test
    fun verifyConnectsSubscribesAndDisconnects() = runTest {
        val gateway = RecordingGateway()
        val verifier = MqttConnectionVerifier { gateway }

        verifier.verify(
            config = sampleConfig(),
            topics = listOf(
                TopicSubscription(
                    topic = "alerts/frontdoor",
                    displayName = "Front door",
                    qos = 1
                )
            )
        )

        assertEquals(
            listOf(
                "connect:tcp://broker:1883",
                "subscribe:alerts/frontdoor:1",
                "disconnect"
            ),
            gateway.events
        )
    }

    private fun sampleConfig() = BrokerConfig(
        serverUri = "tcp://broker:1883",
        clientId = "android-test",
        updatedAtEpochMillis = 1L
    )
}

private class RecordingGateway : MqttClientGateway {
    val events = mutableListOf<String>()
    override val connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    override suspend fun connect(config: MqttConnectionConfig) {
        events += "connect:${config.serverUri}"
    }

    override suspend fun disconnect() {
        events += "disconnect"
    }

    override suspend fun subscribe(topic: String, qos: Int) {
        events += "subscribe:$topic:$qos"
    }

    override suspend fun unsubscribe(topic: String) = Unit

    override fun setListener(listener: suspend (MqttIncomingMessage) -> Unit) = Unit
}
