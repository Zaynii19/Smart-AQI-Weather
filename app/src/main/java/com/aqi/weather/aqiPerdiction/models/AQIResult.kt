package com.aqi.weather.aqiPerdiction.models

data class AQIResult(
    val aqi: Int,
    val label: String,
    val color: Int,
    val healthImpact: String,
    val recommendedAction: String
)