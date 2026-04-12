package com.example.mqttandroid.notifications

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MqttNotificationFactoryTest {
    @Test
    fun topicNotificationRequiresTopicOptIn() {
        val decision = MqttNotificationDecision()

        assertTrue(decision.shouldNotify(notificationsEnabled = true))
        assertFalse(decision.shouldNotify(notificationsEnabled = false))
    }
}
