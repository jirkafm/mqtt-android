# Anonymous MQTT Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow MQTT anonymous access when username/password are omitted, and ignore a password if no username is provided.

**Architecture:** Keep the setup UI permissive and normalize credentials at the MQTT transport boundary. `PahoMqttClientGateway` will omit both `userName` and `password` in `MqttConnectOptions` whenever the username is blank or null, so all connection paths share the same rule.

**Tech Stack:** Kotlin, Eclipse Paho MQTT, JUnit.

---

### Task 1: Add auth-normalization tests

**Files:**
- Create: `app/src/test/java/com/example/mqttandroid/mqtt/PahoMqttClientGatewayTest.kt`
- Modify: `app/src/main/java/com/example/mqttandroid/mqtt/PahoMqttClientGateway.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun connectOptionsOmitCredentialsWhenUsernameMissing() {
    val options = MqttConnectionConfig(
        serverUri = "tcp://broker:1883",
        clientId = "android-test",
        username = null,
        password = "secret"
    ).toConnectOptionsForTest()

    assertNull(options.userName)
    assertNull(options.password)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.mqtt.PahoMqttClientGatewayTest`
Expected: FAIL because the test helper does not exist and/or password is still populated.

- [ ] **Step 3: Write minimal implementation**

```kotlin
private fun MqttConnectionConfig.normalizedCredentials(): Pair<String?, CharArray?> {
    val normalizedUsername = username?.trim().takeUnless { it.isNullOrEmpty() }
    if (normalizedUsername == null) return null to null
    return normalizedUsername to password?.toCharArray()
}
```

Apply that result inside `toConnectOptions()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.example.mqttandroid.mqtt.PahoMqttClientGatewayTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mqttandroid/mqtt/PahoMqttClientGateway.kt app/src/test/java/com/example/mqttandroid/mqtt/PahoMqttClientGatewayTest.kt
git commit -m "fix: support anonymous mqtt auth"
```
