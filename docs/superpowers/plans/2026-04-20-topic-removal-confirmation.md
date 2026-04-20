# Topic Removal Confirmation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require explicit confirmation before a saved topic is removed from the setup screen.

**Architecture:** Keep deletion behavior in `SetupScreenRoute` unchanged and add a local `AlertDialog` confirmation flow inside `SetupScreen`. The UI stores the selected `SavedTopicUiModel` as pending deletion, opens the dialog from the topic card, and only calls `onDeleteTopic(topic.id)` after the user confirms.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Compose UI tests, coroutines.

---

### Task 1: Add failing UI coverage for the confirmation dialog

**Files:**
- Modify: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun removeTopicRequiresConfirmationBeforeDeleteCallbackRuns() {
    var deletedTopicId: Long? = null

    composeRule.setContent {
        MqttAndroidTheme {
            SetupScreen(
                state = initialSetupUiState(defaultClientId = "android-test-id").copy(
                    savedTopics = listOf(
                        SavedTopicUiModel(
                            id = 7L,
                            topic = "alerts/frontdoor",
                            displayName = "Front Door",
                            qos = 1,
                            unreadCount = 0,
                            lastError = null
                        )
                    )
                ),
                onHostChanged = {},
                onPortChanged = {},
                onClientIdChanged = {},
                onUsernameChanged = {},
                onPasswordChanged = {},
                onTlsChanged = {},
                onSaveBroker = {},
                onTopicChanged = {},
                onTopicDisplayNameChanged = {},
                onTopicQosChanged = {},
                onAddTopic = {},
                onDeleteTopic = { deletedTopicId = it },
                onToggleTopicHistory = {},
                onMarkTopicRead = {},
                onClearTopicHistory = {},
                onStartSync = {},
                onStopSync = {}
            )
        }
    }

    composeRule.onNodeWithText("Remove").performClick()
    composeRule.onNodeWithText("Front Door").assertIsDisplayed()
    composeRule.onNodeWithText("Cancel").performClick()
    org.junit.Assert.assertEquals(null, deletedTopicId)

    composeRule.onNodeWithText("Remove").performClick()
    composeRule.onAllNodesWithText("Remove")[1].performClick()
    org.junit.Assert.assertEquals(7L, deletedTopicId)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest#removeTopicRequiresConfirmationBeforeDeleteCallbackRuns`
Expected: FAIL because tapping `Remove` currently calls `onDeleteTopic` immediately and no confirmation dialog exists.

- [ ] **Step 3: Write minimal implementation**

```kotlin
var pendingDeletionTopic by remember { mutableStateOf<SavedTopicUiModel?>(null) }

Button(
    onClick = { pendingDeletionTopic = topic },
    enabled = controlsEnabled
) {
    Text("Remove")
}

pendingDeletionTopic?.let { pendingTopic ->
    AlertDialog(
        onDismissRequest = { pendingDeletionTopic = null },
        title = { Text("Remove topic?") },
        text = {
            Text("Remove ${pendingTopic.displayName} (${pendingTopic.topic})?")
        },
        confirmButton = {
            Button(onClick = {
                onDeleteTopic(pendingTopic.id)
                pendingDeletionTopic = null
            }) {
                Text("Remove")
            }
        },
        dismissButton = {
            Button(onClick = { pendingDeletionTopic = null }) {
                Text("Cancel")
            }
        }
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest#removeTopicRequiresConfirmationBeforeDeleteCallbackRuns`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
SCM_OCI_PROFILE_NAME=jikaplan-phx ~/bin/scm-git add app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt
SCM_OCI_PROFILE_NAME=jikaplan-phx ~/bin/scm-git commit -m "feat: confirm topic removal"
```

### Task 2: Verify the new interaction alongside existing setup-screen coverage

**Files:**
- Modify: `app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt`

- [ ] **Step 1: Keep existing tests aligned with the dialog flow**

```kotlin
// No production code change in this step.
// Adjust only if the new dialog introduces ambiguous node selection in shared tests.
```

- [ ] **Step 2: Run the focused test suite**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.mqttandroid.ui.setup.SetupScreenTest`
Expected: PASS for the existing setup-screen instrumentation tests plus the new confirmation test.

- [ ] **Step 3: Run the relevant unit test target**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.ui.setup.SetupTopicHistoryStateTest`
Expected: PASS to confirm the topic setup state helpers still behave correctly.

- [ ] **Step 4: Review the implementation for YAGNI**

```kotlin
// Ensure no SetupUiState or repository API changes were added.
// Ensure pending deletion stays local to SetupScreen.
```

- [ ] **Step 5: Commit**

```bash
SCM_OCI_PROFILE_NAME=jikaplan-phx ~/bin/scm-git add app/src/androidTest/java/com/example/mqttandroid/ui/setup/SetupScreenTest.kt app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt
SCM_OCI_PROFILE_NAME=jikaplan-phx ~/bin/scm-git commit -m "test: verify topic removal confirmation flow"
```
