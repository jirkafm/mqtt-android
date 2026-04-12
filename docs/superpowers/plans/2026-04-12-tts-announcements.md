# TTS Announcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Speak a short TTS announcement when a new MQTT message arrives, without reading payload contents.

**Architecture:** Add a small service-owned TTS helper that announces `New event on <display name or topic>` for each incoming message. The helper owns `TextToSpeech` initialization and shutdown, while `MqttSyncService` invokes it alongside existing notification/history handling.

**Tech Stack:** Kotlin, Android TextToSpeech, foreground service, JUnit.

---

### Task 1: Add announcement text coverage and wire a TTS helper into the service

**Files:**
- Create: `app/src/main/java/com/example/mqttandroid/speech/MqttEventSpeaker.kt`
- Create: `app/src/test/java/com/example/mqttandroid/speech/MqttEventSpeakerTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/service/MqttSyncService.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun buildIncomingEventAnnouncementUsesDisplayNameWhenPresent() {
    assertEquals(
        "New event on Front Door",
        buildIncomingEventAnnouncement(topicLabel = "Front Door", topic = "alerts/frontdoor")
    )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.speech.MqttEventSpeakerTest`
Expected: FAIL with unresolved reference to `buildIncomingEventAnnouncement`

- [ ] **Step 3: Write minimal implementation**

```kotlin
internal fun buildIncomingEventAnnouncement(topicLabel: String, topic: String): String {
    val spokenTarget = topicLabel.ifBlank { topic }
    return "New event on $spokenTarget"
}
```

Add a `MqttEventSpeaker` wrapper that initializes `TextToSpeech`, speaks announcements, and shuts down in `onDestroy()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.speech.MqttEventSpeakerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/speech/MqttEventSpeaker.kt app/src/main/java/com/example/mqttandroid/service/MqttSyncService.kt app/src/test/java/com/example/mqttandroid/speech/MqttEventSpeakerTest.kt
git commit -m "feat: announce mqtt events with tts"
```
