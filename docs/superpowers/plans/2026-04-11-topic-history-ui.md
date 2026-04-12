# Topic History UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users open a topic, read its stored messages, and explicitly mark all messages in that topic as read from the setup screen.

**Architecture:** Extend `SetupScreenRoute` with one expanded topic at a time plus a collected message-history flow for that topic. `SetupScreen` will render an inline expandable history block under the selected topic row, including `Mark all as read` and `Clear all messages` actions backed by the existing `MessageRepository` methods.

**Tech Stack:** Kotlin, Jetpack Compose, Room repositories, coroutines, Compose UI tests.

---

### Task 1: Add failing UI coverage for expanded topic history

**Files:**
- Modify: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun expandedTopicShowsMessageHistoryAndActions() {
    composeRule.setContent {
        MqttAndroidTheme {
            SetupScreen(
                state = initialSetupUiState("android-test-id").copy(
                    savedTopics = listOf(SavedTopicUiModel(id = 1, topic = "alerts/frontdoor", displayName = "Front Door", qos = 1, unreadCount = 5, lastError = null)),
                    expandedTopicId = 1,
                    expandedTopicMessages = listOf(TopicMessageUiModel(id = 11, payloadPreview = "door opened", receivedAtLabel = "14:27:31", isRead = false))
                ),
                ...
            )
        }
    }

    composeRule.onNodeWithText("door opened").assertIsDisplayed()
    composeRule.onNodeWithText("Mark all as read").assertIsDisplayed()
    composeRule.onNodeWithText("Clear all messages").assertIsDisplayed()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: FAIL because expanded-topic UI models and actions do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```kotlin
data class TopicMessageUiModel(...)
data class SetupUiState(
    ...,
    val expandedTopicId: Long? = null,
    val expandedTopicMessages: List<TopicMessageUiModel> = emptyList()
)
```

Render the inline message list and buttons when `state.expandedTopicId == topic.id`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt
git commit -m "feat: show topic message history inline"
```

### Task 2: Wire repository-backed history, mark-read, and clear actions

**Files:**
- Modify: `app/src/main/java/com/example/mqttandroid/MainActivity.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`

- [ ] **Step 1: Write the failing test**

Use the existing repository behavior as backing expectations and add UI assertions for the visible actions labels if needed.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: FAIL until handlers are wired.

- [ ] **Step 3: Write minimal implementation**

```kotlin
LaunchedEffect(state.expandedTopicId) {
    val topicId = state.expandedTopicId ?: return@LaunchedEffect
    messageRepository.observeMessages(topicId).collect { messages ->
        state = state.copy(expandedTopicMessages = messages.map(::toUiModel))
    }
}
```

Add handlers for:
- toggling expanded topic
- `messageRepository.markTopicRead(topicId)`
- `messageRepository.clearTopicHistory(topicId)`

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/MainActivity.kt app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt
git commit -m "feat: add mark-read and clear-history topic actions"
```
