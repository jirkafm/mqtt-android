package com.example.mqttandroid.mqtt

import kotlin.math.min
import kotlin.math.pow

class MqttReconnectPolicy(
    private val baseDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 60_000
) {
    fun delayForAttempt(attempt: Int): Long {
        val exponent = attempt.coerceAtLeast(0)
        val delay = baseDelayMillis * 2.0.pow(exponent).toLong()
        return min(delay, maxDelayMillis)
    }
}
