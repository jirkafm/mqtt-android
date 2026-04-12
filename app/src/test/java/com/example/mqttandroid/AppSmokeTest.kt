package com.example.mqttandroid

import org.junit.Test
import com.example.mqttandroid.ui.theme.Blue40
import com.example.mqttandroid.ui.theme.Teal40
import androidx.compose.ui.graphics.toArgb
import kotlin.test.assertEquals

class AppSmokeTest {
    @Test
    fun appIdentityUsesMqttAndroidName() {
        assertEquals("com.example.mqttandroid.MainActivity", MainActivity::class.qualifiedName)
        assertEquals(
            "com.example.mqttandroid.MqttAndroidApplication",
            MqttAndroidApplication::class.qualifiedName
        )
    }

    @Test
    fun themeColorsStayStable() {
        assertEquals(0xFF3F5EBAL.toInt(), Blue40.toArgb())
        assertEquals(0xFF2A7F74L.toInt(), Teal40.toArgb())
    }
}
