# Connect Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Validate broker credentials and topic subscriptions before enabling background listening, and rename the primary action from Start listener to Connect.

**Architecture:** Add a small connection verifier that uses a temporary `MqttClientGateway` instance to connect, subscribe to each active topic, and disconnect. The setup screen route will call that verifier before persisting `syncEnabled = true` and starting `MqttSyncService`, while the UI text changes to reflect the new flow.

**Tech Stack:** Kotlin, Jetpack Compose, coroutines, existing Paho-based `MqttClientGateway`, JUnit, Compose UI tests.

---

### Task 1: Add connection verification domain logic

**Files:**
- Create: `app/src/main/java/com/example/mqttandroid/domain/MqttConnectionVerifier.kt`
- Test: `app/src/test/java/com/example/mqttandroid/domain/MqttConnectionVerifierTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun verifyConnectsSubscribesAndDisconnects() = runTest {
    val gateway = RecordingGateway()
    val verifier = MqttConnectionVerifier { gateway }

    verifier.verify(
        config = sampleConfig(),
        topics = listOf(
            TopicSubscription(topic = "alerts/frontdoor", displayName = "Front door", qos = 1)
        )
    )

    assertEquals(listOf("connect:tcp://broker:1883", "subscribe:alerts/frontdoor:1", "disconnect"), gateway.events)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.domain.MqttConnectionVerifierTest`
Expected: FAIL with unresolved reference to `MqttConnectionVerifier`

- [ ] **Step 3: Write minimal implementation**

```kotlin
class MqttConnectionVerifier(
    private val gatewayFactory: () -> MqttClientGateway
) {
    suspend fun verify(config: BrokerConfig, topics: List<TopicSubscription>) {
        val gateway = gatewayFactory()
        try {
            gateway.connect(config.toConnectionConfig())
            topics.filter { it.subscriptionEnabled }.forEach { topic ->
                gateway.subscribe(topic.topic, topic.qos)
            }
        } finally {
            gateway.disconnect()
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.domain.MqttConnectionVerifierTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/domain/MqttConnectionVerifier.kt app/src/test/java/com/example/mqttandroid/domain/MqttConnectionVerifierTest.kt
git commit -m "feat: add mqtt connection verifier"
```

### Task 2: Wire Connect flow into setup screen

**Files:**
- Modify: `app/src/main/java/com/example/mqttandroid/data/AppContainer.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/MainActivity.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`
- Test: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
composeRule.onNodeWithText("Connect").assertIsDisplayed()
composeRule.onNodeWithText("Start listener").assertDoesNotExist()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: FAIL because the button still says `Start listener`

- [ ] **Step 3: Write minimal implementation**

```kotlin
Button(onClick = if (state.syncEnabled) onStopSync else onStartSync) {
    Text(if (state.syncEnabled) "Disconnect" else "Connect")
}
```

And in the route:

```kotlin
val topics = topicRepository.getTopics().filter { it.subscriptionEnabled }
if (topics.isEmpty()) {
    state = state.copy(statusMessage = "Add at least one topic before connecting.")
    return@launch
}
state = state.copy(statusMessage = "Connecting...")
connectionVerifier.verify(config, topics)
brokerRepository.saveBrokerConfig(config)
MqttSyncService.start(context)
state = state.copy(syncEnabled = true, statusMessage = "Connected.")
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.domain.MqttConnectionVerifierTest`
Expected: PASS

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/data/AppContainer.kt app/src/main/java/com/example/mqttandroid/MainActivity.kt app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt
git commit -m "feat: validate mqtt connection before starting listener"
```
