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

    @Test
    fun nextAnnouncementResultUsesIndividualAnnouncementsForFirstThreeMessages() {
        var burst: TopicAnnouncementBurst? = null

        val first = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 1_000L
        )
        burst = first.nextBurst

        val second = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 2_000L
        )
        burst = second.nextBurst

        val third = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 3_000L
        )

        assertEquals("New event on Front Door", first.announcement)
        assertEquals("New event on Front Door", second.announcement)
        assertEquals("New event on Front Door", third.announcement)
    }

    @Test
    fun nextAnnouncementResultSwitchesToSummaryOnFourthMessageWithinFiveSeconds() {
        var burst: TopicAnnouncementBurst? = null
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 1_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 2_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 3_000L).nextBurst

        val fourth = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 4_000L
        )

        assertEquals("There are 4 messages on Front Door", fourth.announcement)
    }

    @Test
    fun nextAnnouncementResultSuppressesLaterMessagesAfterBurstWasSummarized() {
        var burst: TopicAnnouncementBurst? = null
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 1_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 2_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 3_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 4_000L).nextBurst

        val fifth = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 4_500L
        )

        assertEquals(null, fifth.announcement)
        assertEquals(5, fifth.nextBurst.count)
    }

    @Test
    fun nextAnnouncementResultResetsBurstAfterFiveSecondGap() {
        var burst: TopicAnnouncementBurst? = null
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 1_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 2_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 3_000L).nextBurst
        burst = nextAnnouncementResult(burst, "Front Door", "alerts/frontdoor", 4_000L).nextBurst

        val afterGap = nextAnnouncementResult(
            currentBurst = burst,
            topicLabel = "Front Door",
            topic = "alerts/frontdoor",
            nowEpochMillis = 10_001L
        )

        assertEquals("New event on Front Door", afterGap.announcement)
        assertEquals(1, afterGap.nextBurst.count)
    }

    @Test
    fun buildBurstSummaryAnnouncementFallsBackToTopicWhenDisplayNameIsBlank() {
        assertEquals(
            "There are 4 messages on alerts/frontdoor",
            buildBurstSummaryAnnouncement(
                topicLabel = "",
                topic = "alerts/frontdoor",
                count = 4
            )
        )
    }
}
