# Status Timestamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the last status update time alongside the setup screen status message so users can see when the state last changed.

**Architecture:** Extend `SetupUiState` with `statusUpdatedAtEpochMillis` and render the status line through a small pure formatter. Every place that updates `statusMessage` in `SetupScreenRoute` will also stamp the current time so the displayed line stays in sync.

**Tech Stack:** Kotlin, Jetpack Compose, java.time, JUnit.

---

### Task 1: Add formatter coverage and wire timestamps into setup status updates

**Files:**
- Create: `app/src/test/java/com/example/mqttandroid/ui/setup/SetupStatusFormattingTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun formatStatusLinePrependsLocalTime() {
    assertEquals(
        "14:27:31 Connected.",
        formatStatusLine(
            message = "Connected.",
            statusUpdatedAtEpochMillis = 1_710_173_251_000,
            zoneId = ZoneId.of("Europe/Prague"),
            locale = Locale.US
        )
    )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.ui.setup.SetupStatusFormattingTest`
Expected: FAIL with unresolved reference to `formatStatusLine`

- [ ] **Step 3: Write minimal implementation**

```kotlin
internal fun formatStatusLine(
    message: String,
    statusUpdatedAtEpochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", locale)
    val time = Instant.ofEpochMilli(statusUpdatedAtEpochMillis).atZone(zoneId)
    return "${formatter.format(time)} $message"
}
```

Also add `statusUpdatedAtEpochMillis` to `SetupUiState` and stamp it whenever `statusMessage` changes.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.ui.setup.SetupStatusFormattingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/ui/setup/SetupScreen.kt app/src/test/java/com/example/mqttandroid/ui/setup/SetupStatusFormattingTest.kt
git commit -m "feat: timestamp setup status line"
```
