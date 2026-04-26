package com.aqi.weather.data.remote.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiService {
    val weatherApi: WeatherApi by lazy {
        createRetrofitClient("https://api.openweathermap.org/data/2.5/")
            .create(WeatherApi::class.java)
    }

    val locationApi: LocationApi by lazy {
        createRetrofitClient("https://api.bigdatacloud.net/data/")
            .create(LocationApi::class.java)
    }
}

fun createRetrofitClient(
    baseUrl: String,
    timeoutSeconds: Long = 30,
    enableLogging: Boolean = true
): Retrofit {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .apply {
            if (enableLogging) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
}
