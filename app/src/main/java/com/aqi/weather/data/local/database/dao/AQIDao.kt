package com.aqi.weather.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqi.weather.data.local.database.entity.AQI

@Dao
interface AQIDao {
    @Upsert
    suspend fun upsertAqi(aqi: AQI)

    @Query("SELECT * FROM aqi WHERE userId = :userId")
    suspend fun getAqiByUserId(userId: String): MutableList<AQI>
}