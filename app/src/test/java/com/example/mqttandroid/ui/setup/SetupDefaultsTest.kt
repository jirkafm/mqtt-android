package com.example.mqttandroid.ui.setup

import com.example.mqttandroid.setup.generateStartupClientId
import com.example.mqttandroid.setup.initialSetupUiState
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetupDefaultsTest {
    @Test
    fun initialSetupUiStatePrefillsStandardPortAndStartupClientId() {
        val state = initialSetupUiState(defaultClientId = "android-test-id")

        assertEquals("1883", state.port)
        assertEquals("android-test-id", state.clientId)
    }

    @Test
    fun generateStartupClientIdPrefixesUuidWithAndroid() {
        val id = generateStartupClientId(
            uuid = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
        )

        assertEquals("android-12345678-1234-1234-1234-1234567890ab", id)
        assertTrue(id.startsWith("android-"))
    }
}
