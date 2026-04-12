# Connecting Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make MQTT connection attempts visibly active by disabling the full setup form and showing loading feedback until the attempt succeeds or fails.

**Architecture:** Extend `SetupUiState` with a transient `isConnecting` flag owned by `SetupScreenRoute`. The route toggles that flag around the connection verifier call, and `SetupScreen` renders disabled controls plus an inline loading indicator in the primary button while the flag is true.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, coroutines, Compose UI tests.

---

### Task 1: Add a failing UI test for connecting feedback

**Files:**
- Modify: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun setupScreenShowsConnectingFeedback() {
    composeRule.setContent {
        MqttAndroidTheme {
            SetupScreen(
                state = initialSetupUiState(defaultClientId = "android-test-id").copy(isConnecting = true),
                ...
            )
        }
    }

    composeRule.onNodeWithText("Connecting...").assertIsDisplayed()
    composeRule.onNodeWithTag("connect-progress").assertIsDisplayed()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: FAIL because `isConnecting` and the progress indicator do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```kotlin
data class SetupUiState(
    ...,
    val isConnecting: Boolean = false
)

CircularProgressIndicator(
    modifier = Modifier
        .size(18.dp)
        .testTag("connect-progress"),
    strokeWidth = 2.dp
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt
git commit -m "feat: show connecting feedback in setup screen"
```

### Task 2: Disable the full form during connection attempts

**Files:**
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`
- Test: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
composeRule.onNodeWithText("Save broker").assertIsNotEnabled()
composeRule.onNodeWithText("Add topic").assertIsNotEnabled()
composeRule.onNodeWithText("Connecting...").assertIsNotEnabled()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: FAIL because controls are still enabled.

- [ ] **Step 3: Write minimal implementation**

```kotlin
val controlsEnabled = !state.isConnecting

OutlinedTextField(..., enabled = controlsEnabled)
Button(onClick = onSaveBroker, enabled = controlsEnabled)
Button(onClick = onAddTopic, enabled = controlsEnabled)
Button(onClick = { onDeleteTopic(topic.id) }, enabled = controlsEnabled)
Button(
    onClick = if (state.syncEnabled) onStopSync else onStartSync,
    enabled = !state.isConnecting
) { ... }
```

And in the route:

```kotlin
state = state.copy(isConnecting = true, statusMessage = "Connecting...")
runCatching { connectionVerifier.verify(config, topics) }
    .onSuccess { state = state.copy(isConnecting = false, syncEnabled = true, statusMessage = "Connected.") }
    .onFailure { state = state.copy(isConnecting = false, syncEnabled = false, statusMessage = error.message ?: "Connection failed.") }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt
git commit -m "feat: disable setup form while connecting"
```
