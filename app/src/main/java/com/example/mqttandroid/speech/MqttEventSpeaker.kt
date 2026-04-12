package com.example.mqttandroid.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

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

internal fun buildIncomingEventAnnouncement(
    topicLabel: String,
    topic: String
): String {
    val spokenTarget = topicLabel.ifBlank { topic }
    return "New event on $spokenTarget"
}
