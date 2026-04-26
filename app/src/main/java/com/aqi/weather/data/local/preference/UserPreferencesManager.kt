package com.aqi.weather.data.local.preference


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// Extension for SharedPreferences
val Context.sharedPrefs: SharedPreferences
    get() = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

class UserPreferencesManager(private val context: Context) {

    fun savePreference(
        userType: String? = null,
        userId: String? = null
    ) {
        context.sharedPrefs.edit().apply {
            userType?.let { putString("userType", it) }
            userId?.let { putString("userId", it) }
            apply()
        }
    }

    // Getters
    val userId: String get() = context.sharedPrefs.getString("userId", "") ?: ""
    val userType: String get() = context.sharedPrefs.getString("userType", "") ?: ""

    fun clearAllPreferences() {
        context.sharedPrefs.edit { clear() }
    }
}