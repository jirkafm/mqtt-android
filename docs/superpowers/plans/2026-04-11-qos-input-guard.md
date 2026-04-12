# QoS Input Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restrict the QoS setup field to a single allowed digit so invalid multi-character input never enters UI state.

**Architecture:** Add a small pure sanitizer in `SetupScreen.kt` that accepts only `0`, `1`, or `2` as a single-character value. The setup route will use that sanitizer inside `onTopicQosChanged`, while existing save-time validation remains as a second guard.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit.

---

### Task 1: Add QoS sanitizer coverage and wire it into state updates

**Files:**
- Create: `app/src/test/java/com/example/mqttandroid/ui/setup/SetupScreenQoSTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun sanitizeTopicQosInputRejectsMultipleDigitsAndInvalidValues() {
    assertEquals("2", sanitizeTopicQosInput(current = "2", input = "20"))
    assertEquals("1", sanitizeTopicQosInput(current = "1", input = "9"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.ui.setup.SetupScreenQoSTest`
Expected: FAIL with unresolved reference to `sanitizeTopicQosInput`

- [ ] **Step 3: Write minimal implementation**

```kotlin
internal fun sanitizeTopicQosInput(current: String, input: String): String {
    if (input.isEmpty()) return ""
    if (input.length != 1) return current
    return input.takeIf { it in setOf("0", "1", "2") } ?: current
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.ui.setup.SetupScreenQoSTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt app/src/test/java/com/example/mqttandroid/ui/setup/SetupScreenQoSTest.kt
git commit -m "fix: constrain qos input"
```
