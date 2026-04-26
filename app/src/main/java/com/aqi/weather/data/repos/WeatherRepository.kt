package com.aqi.weather.data.repos

import com.aqi.weather.BuildConfig
import com.aqi.weather.data.remote.api.ApiService
import com.aqi.weather.data.remote.dto.WeatherResponse

class WeatherRepository {
    private val apiKey = BuildConfig.WEATHER_API_KEY

    suspend fun getWeatherData(cityName: String): Result<WeatherResponse> {
        return try {
            val response = ApiService.weatherApi.getWeatherData(cityName, apiKey, "metric")
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}