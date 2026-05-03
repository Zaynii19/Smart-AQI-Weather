package com.aqi.weather.data.local.database.entity

import androidx.room.Entity

@Entity(
    tableName = "aqi",
    primaryKeys = ["userId", "date"] // Both together must be unique
)
data class AQI(
    val userId: String,
    val date: String,
    val updatedAt: String,
    val aqi: Int,
    val label: String,
    val color: Int,
    val healthImpact: String,
    val recommendedAction: String
)