package com.example.mqttandroid.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mqttandroid.data.db.entity.BrokerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerConfigDao {
    @Query("SELECT * FROM broker_config WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BrokerConfigEntity?

    @Query("SELECT * FROM broker_config WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<BrokerConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: BrokerConfigEntity)

    @Query("DELETE FROM broker_config WHERE id = :id")
    suspend fun deleteById(id: Long)
}
