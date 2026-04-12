package com.example.mqttandroid.speech

import kotlin.test.Test
import kotlin.test.assertEquals

class MqttEventSpeakerTest {
    @Test
    fun buildIncomingEventAnnouncementUsesDisplayNameWhenPresent() {
        assertEquals(
            "New event on Front Door",
            buildIncomingEventAnnouncement(
                topicLabel = "Front Door",
                topic = "alerts/frontdoor"
            )
        )
    }

    @Test
    fun buildIncomingEventAnnouncementFallsBackToTopic() {
        assertEquals(
            "New event on alerts/frontdoor",
            buildIncomingEventAnnouncement(
                topicLabel = "",
                topic = "alerts/frontdoor"
            )
        )
    }
}
