package com.aqi.weather.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

val Context.sharedPrefs: SharedPreferences
    get() = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

class UserPreferencesManager(private val context: Context) {

    fun savePreference(
        userType: String? = null,
        userId: String? = null,
        aqiThreshold: Int? = null,
        notificationEnabled: Boolean? = null
    ) {
        context.sharedPrefs.edit().apply {
            userType?.let { putString("userType", it) }
            userId?.let { putString("userId", it) }
            aqiThreshold?.let { putInt("aqiThreshold", it) }
            notificationEnabled?.let { putBoolean("notificationEnabled", it) }
            apply()
        }
    }

    // Getters
    val userId: String get() = context.sharedPrefs.getString("userId", "") ?: ""
    val userType: String get() = context.sharedPrefs.getString("userType", "") ?: ""
    val aqiThreshold: Int get() = context.sharedPrefs.getInt("aqiThreshold", 100)
    val notificationEnabled: Boolean get() = context.sharedPrefs.getBoolean("notificationEnabled", true)

    fun clearAllPreferences() {
        context.sharedPrefs.edit { clear() }
    }
}