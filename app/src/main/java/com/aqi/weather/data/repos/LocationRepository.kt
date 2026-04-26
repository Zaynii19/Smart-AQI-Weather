package com.aqi.weather.data.repos

import com.aqi.weather.BuildConfig
import com.aqi.weather.data.remote.api.ApiService
import com.aqi.weather.data.remote.dto.LocationResponse

class LocationRepository {
    private val apiKey = BuildConfig.LOCATION_API_KEY

    suspend fun getLocationData(latitude: String, longitude: String): Result<LocationResponse> {
        return try {
            val response = ApiService.locationApi.getLocationData(apiKey, latitude, longitude,"en")
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