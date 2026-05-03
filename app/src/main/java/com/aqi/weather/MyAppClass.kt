package com.aqi.weather

import android.app.Application
import com.aqi.weather.data.local.preference.UserPreferencesManager

class MyAppClass : Application() {

    override fun onCreate() {
        super.onCreate()

        // Save AQI threshold to SharedPreferences
        val prefsManager = UserPreferencesManager(this)
        val aqiThreshold = prefsManager.aqiThreshold
        val notificationEnabled = prefsManager.notificationEnabled
        prefsManager.savePreference(aqiThreshold = aqiThreshold, notificationEnabled = notificationEnabled)
    }
}