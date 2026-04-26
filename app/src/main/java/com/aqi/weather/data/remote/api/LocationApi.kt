package com.aqi.weather.data.remote.api

import com.aqi.weather.data.remote.dto.LocationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApi {
    @GET("reverse-geocode")
    suspend fun getLocationData(
        @Query("key") key: String,
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("localityLanguage") localityLanguage: String
    ): Response<LocationResponse>
}