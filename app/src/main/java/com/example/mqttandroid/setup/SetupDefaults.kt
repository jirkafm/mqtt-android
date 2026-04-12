package com.example.mqttandroid.setup

import com.example.mqttandroid.ui.setup.SetupUiState
import java.util.UUID

const val DEFAULT_MQTT_PORT = "1883"

fun initialSetupUiState(defaultClientId: String): SetupUiState =
    SetupUiState(
        port = DEFAULT_MQTT_PORT,
        clientId = defaultClientId
    )

fun generateStartupClientId(uuid: UUID = UUID.randomUUID()): String = "android-$uuid"
