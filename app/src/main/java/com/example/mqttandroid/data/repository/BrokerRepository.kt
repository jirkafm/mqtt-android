package com.example.mqttandroid.data.repository

import com.example.mqttandroid.data.db.dao.BrokerConfigDao
import com.example.mqttandroid.data.db.entity.BrokerConfigEntity
import com.example.mqttandroid.data.model.BrokerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrokerRepository(
    private val brokerConfigDao: BrokerConfigDao
) {
    fun observeBrokerConfig(): Flow<BrokerConfig?> =
        brokerConfigDao.observeById(BrokerConfigEntity.SINGLETON_ID)
            .map { it?.toModel() }

    suspend fun getBrokerConfig(): BrokerConfig? =
        brokerConfigDao.getById(BrokerConfigEntity.SINGLETON_ID)?.toModel()

    suspend fun saveBrokerConfig(config: BrokerConfig) {
        brokerConfigDao.upsert(config.toEntity())
    }

    suspend fun clearBrokerConfig() {
        brokerConfigDao.deleteById(BrokerConfigEntity.SINGLETON_ID)
    }
}

private fun BrokerConfigEntity.toModel() = BrokerConfig(
    serverUri = serverUri,
    clientId = clientId,
    username = username,
    password = password,
    tlsEnabled = tlsEnabled,
    cleanSession = cleanSession,
    keepAliveSeconds = keepAliveSeconds,
    autoReconnect = autoReconnect,
    syncEnabled = syncEnabled,
    updatedAtEpochMillis = updatedAtEpochMillis
)

private fun BrokerConfig.toEntity() = BrokerConfigEntity(
    id = BrokerConfigEntity.SINGLETON_ID,
    serverUri = serverUri,
    clientId = clientId,
    username = username,
    password = password,
    tlsEnabled = tlsEnabled,
    cleanSession = cleanSession,
    keepAliveSeconds = keepAliveSeconds,
    autoReconnect = autoReconnect,
    syncEnabled = syncEnabled,
    updatedAtEpochMillis = updatedAtEpochMillis
)
