package com.example.mqttandroid.mqtt

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class PahoMqttClientGatewayTest {
    @Test
    fun connectOptionsOmitCredentialsWhenUsernameMissing() {
        val options = MqttConnectionConfig(
            serverUri = "tcp://broker:1883",
            clientId = "android-test",
            username = null,
            password = "secret"
        ).toConnectOptionsForTest()

        assertNull(options.userName)
        assertNull(options.password)
    }

    @Test
    fun connectOptionsKeepCredentialsWhenUsernamePresent() {
        val options = MqttConnectionConfig(
            serverUri = "tcp://broker:1883",
            clientId = "android-test",
            username = "demo",
            password = "secret"
        ).toConnectOptionsForTest()

        kotlin.test.assertEquals("demo", options.userName)
        assertContentEquals("secret".toCharArray(), options.password)
    }
}
