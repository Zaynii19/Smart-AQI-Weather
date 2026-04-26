package com.aqi.weather.aqiPerdiction.repos

import android.content.Context
import android.util.Log
import com.aqi.weather.aqiPerdiction.AQIMapper
import com.aqi.weather.aqiPerdiction.AQIModel
import com.aqi.weather.aqiPerdiction.FeatureEngineer
import com.aqi.weather.aqiPerdiction.Normalizer
import com.aqi.weather.aqiPerdiction.models.Scaler
import com.aqi.weather.aqiPerdiction.models.AQIResult
import com.aqi.weather.data.remote.dto.WeatherResponse
import com.aqi.weather.util.AssetUtils

class AQIRepository(context: Context) {
    private val scaler: Scaler
    private val model: AQIModel = AQIModel(context)

    init {
        val json = AssetUtils.loadJSON(context, "scaler_latlon.json")
        scaler = Normalizer.fromJSON(json)
    }

    suspend fun predictAQI(weather: WeatherResponse): AQIResult {
        val features = FeatureEngineer.buildFeatures(weather)
        val normalized = Normalizer.normalize(features, scaler)
        val aqiValue = model.predict(normalized)
        Log.d("LocalAqiRepository", "AQI Value: $aqiValue")
        return AQIMapper.map(aqiValue)
    }

    fun clear() {
        model.close()
    }
}