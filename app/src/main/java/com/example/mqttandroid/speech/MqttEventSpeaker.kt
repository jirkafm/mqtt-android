package com.example.mqttandroid.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

private const val BURST_WINDOW_MILLIS = 5_000L
private const val INDIVIDUAL_ANNOUNCEMENT_LIMIT = 3

class MqttEventSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val applicationContext = context.applicationContext
    private val pendingAnnouncements = mutableListOf<String>()
    private var initialized = false
    private var available = true
    private val textToSpeech = TextToSpeech(applicationContext, this)

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        if (!initialized) {
            available = false
            pendingAnnouncements.clear()
            return
        }

        val languageResult = textToSpeech.setLanguage(Locale.getDefault())
        available = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (!available) {
            pendingAnnouncements.clear()
            return
        }

        pendingAnnouncements.forEach(::speakNow)
        pendingAnnouncements.clear()
    }

    fun announceIncomingEvent(
        topicLabel: String,
        topic: String
    ) {
        if (!available) return
        val announcement = buildIncomingEventAnnouncement(topicLabel, topic)
        if (!initialized) {
            pendingAnnouncements += announcement
            return
        }
        speakNow(announcement)
    }

    fun shutdown() {
        pendingAnnouncements.clear()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun speakNow(announcement: String) {
        textToSpeech.speak(
            announcement,
            TextToSpeech.QUEUE_ADD,
            null,
            announcement.hashCode().toString()
        )
    }
}

internal data class TopicAnnouncementBurst(
    val count: Int,
    val lastEventAtEpochMillis: Long,
    val summarized: Boolean
)

internal data class TopicAnnouncementResult(
    val announcement: String?,
    val nextBurst: TopicAnnouncementBurst
)

internal fun nextAnnouncementResult(
    currentBurst: TopicAnnouncementBurst?,
    topicLabel: String,
    topic: String,
    nowEpochMillis: Long
): TopicAnnouncementResult {
    val activeBurst = if (
        currentBurst == null ||
        nowEpochMillis - currentBurst.lastEventAtEpochMillis > BURST_WINDOW_MILLIS
    ) {
        TopicAnnouncementBurst(
            count = 0,
            lastEventAtEpochMillis = nowEpochMillis,
            summarized = false
        )
    } else {
        currentBurst
    }

    val nextCount = activeBurst.count + 1
    val nextBurst = TopicAnnouncementBurst(
        count = nextCount,
        lastEventAtEpochMillis = nowEpochMillis,
        summarized = nextCount > INDIVIDUAL_ANNOUNCEMENT_LIMIT
    )

    val announcement = when {
        nextCount <= INDIVIDUAL_ANNOUNCEMENT_LIMIT ->
            buildIncomingEventAnnouncement(topicLabel, topic)
        nextCount == INDIVIDUAL_ANNOUNCEMENT_LIMIT + 1 ->
            buildBurstSummaryAnnouncement(topicLabel, topic, nextCount)
        else -> null
    }

    return TopicAnnouncementResult(
        announcement = announcement,
        nextBurst = nextBurst
    )
}

internal fun buildIncomingEventAnnouncement(
    topicLabel: String,
    topic: String
): String {
    val spokenTarget = topicLabel.ifBlank { topic }
    return "New event on $spokenTarget"
}

internal fun buildBurstSummaryAnnouncement(
    topicLabel: String,
    topic: String,
    count: Int
): String {
    val spokenTarget = topicLabel.ifBlank { topic }
    return "There are $count messages on $spokenTarget"
}
