package com.example.mqttandroid.ui.setup

import kotlin.test.Test
import kotlin.test.assertEquals

class SetupScreenQoSTest {
    @Test
    fun sanitizeTopicQosInputRejectsMultipleDigitsAndInvalidValues() {
        assertEquals("2", sanitizeTopicQosInput(current = "2", input = "20"))
        assertEquals("1", sanitizeTopicQosInput(current = "1", input = "9"))
    }

    @Test
    fun sanitizeTopicQosInputAcceptsSingleAllowedDigit() {
        assertEquals("0", sanitizeTopicQosInput(current = "1", input = "0"))
        assertEquals("2", sanitizeTopicQosInput(current = "1", input = "2"))
    }
}
