package com.example.mqttandroid.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals

class MqttReconnectPolicyTest {
    @Test
    fun delayCapsAtMaximumBackoff() {
        val policy = MqttReconnectPolicy(
            baseDelayMillis = 1_000,
            maxDelayMillis = 60_000
        )

        assertEquals(60_000, policy.delayForAttempt(10))
    }
}
