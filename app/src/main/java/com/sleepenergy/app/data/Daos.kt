package com.sleepenergy.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface NightDao {
    @Query("SELECT * FROM nights WHERE nightEpochDay >= :from ORDER BY nightEpochDay")
    fun observeFrom(from: Long): Flow<List<NightEntity>>

    @Query("SELECT * FROM nights WHERE nightEpochDay = :night")
    suspend fun get(night: Long): NightEntity?

    @Query("SELECT * FROM nights WHERE nightEpochDay BETWEEN :from AND :to ORDER BY nightEpochDay")
    suspend fun between(from: Long, to: Long): List<NightEntity>

    @Query("SELECT * FROM nights ORDER BY nightEpochDay")
    suspend fun all(): List<NightEntity>

    @Upsert
    suspend fun upsert(night: NightEntity)
}
