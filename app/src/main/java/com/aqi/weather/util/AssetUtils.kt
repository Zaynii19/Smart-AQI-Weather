package com.aqi.weather.util

import android.content.Context

object AssetUtils {
    fun loadJSON(context: Context, fileName: String): String {
        return context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
    }
}