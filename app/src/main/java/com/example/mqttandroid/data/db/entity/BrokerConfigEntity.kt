package com.example.mqttandroid.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "broker_config")
data class BrokerConfigEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val serverUri: String,
    val clientId: String,
    val username: String? = null,
    val password: String? = null,
    val tlsEnabled: Boolean = false,
    val cleanSession: Boolean = true,
    val keepAliveSeconds: Int = 60,
    val autoReconnect: Boolean = true,
    val syncEnabled: Boolean = false,
    val updatedAtEpochMillis: Long
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
