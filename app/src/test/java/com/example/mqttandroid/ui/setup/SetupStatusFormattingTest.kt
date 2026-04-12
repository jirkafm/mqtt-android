package com.example.mqttandroid.ui.setup

import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class SetupStatusFormattingTest {
    @Test
    fun formatStatusLinePrependsLocalTime() {
        assertEquals(
            "17:07:31 Connected.",
            formatStatusLine(
                message = "Connected.",
                statusUpdatedAtEpochMillis = 1_710_173_251_000,
                zoneId = ZoneId.of("Europe/Prague"),
                locale = Locale.US
            )
        )
    }
}
