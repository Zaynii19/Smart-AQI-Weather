package com.aqi.weather.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aqi",
    indices = [Index(value = ["userId"], unique = true)]
)
data class AQI(
    @PrimaryKey
    val userId: String,
    val date: String,
    val updatedAt: String,
    val aqi: Int,
    val label: String,
    val color: Int,
    val healthImpact: String,
    val recommendedAction: String
)