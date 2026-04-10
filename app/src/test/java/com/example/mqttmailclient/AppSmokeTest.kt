package com.example.mqttmailclient

import org.junit.Test
import kotlin.test.assertEquals

class AppSmokeTest {
    @Test
    fun buildConfigUsesExpectedApplicationId() {
        assertEquals("com.example.mqttmailclient", BuildConfig.APPLICATION_ID)
    }
}
