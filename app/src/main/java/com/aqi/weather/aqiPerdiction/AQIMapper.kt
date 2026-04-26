package com.aqi.weather.aqiPerdiction

import android.graphics.Color
import com.aqi.weather.aqiPerdiction.models.AQIResult

object AQIMapper {
    fun map(aqiClass: Int): AQIResult {
        val actualAQI = when (aqiClass) {
            1 -> 25   // midpoint of 0-50
            2 -> 75   // midpoint of 51-100
            3 -> 125  // midpoint of 101-150
            4 -> 175  // midpoint of 151-200
            5 -> 250  // midpoint of 201-300
            else -> 400 // midpoint-ish of 301-500
        }

        return when (aqiClass) {
            1 -> AQIResult(actualAQI, "Good", Color.GREEN,
                "No health risk.",
                "Safe to enjoy outdoor activities.")
            2 -> AQIResult(actualAQI, "Moderate", Color.YELLOW,
                "Minor risk for a few sensitive groups (children, elderly, people with asthma).",
                "Sensitive people should reduce long outdoor stays.")
            3 -> AQIResult(actualAQI, "Unhealthy for Sensitive Groups", Color.rgb(255, 165, 0),
                "Some risk for sensitive groups.",
                "Limit outdoor activity if you're in a sensitive group.")
            4 -> AQIResult(actualAQI, "Unhealthy", Color.RED,
                "Health effects possible for all; worse for sensitive groups.",
                "Avoid outdoor exercise; stay indoors if possible.")
            5 -> AQIResult(actualAQI, "Very Unhealthy", Color.rgb(128, 0, 128),
                "Serious health risks for everyone.",
                "Stay indoors; use air purifiers if available.")
            else -> AQIResult(actualAQI, "Hazardous", Color.rgb(128, 0, 0),
                "Emergency: high risk for all.",
                "Avoid all outdoor activities; follow government alerts.")
        }
    }
}