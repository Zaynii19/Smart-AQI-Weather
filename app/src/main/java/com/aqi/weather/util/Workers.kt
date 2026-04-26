package com.aqi.weather.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aqi.weather.aqiPerdiction.repos.AQIRepository
import com.aqi.weather.data.local.database.AQIDatabase
import com.aqi.weather.data.local.database.entity.AQI
import com.aqi.weather.data.local.database.repository.LocalAqiRepository
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.data.repos.LocationRepository
import com.aqi.weather.data.repos.RemoteAqiRepository
import com.aqi.weather.data.repos.WeatherRepository
import com.aqi.weather.receivers.DailyNotificationReceiver
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class AqiSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val locationRepository = LocationRepository()
    private val weatherRepository = WeatherRepository()
    private lateinit var aqiRepository: AQIRepository
    private val remoteAqiRepository = RemoteAqiRepository()
    private lateinit var localAqiRepository: LocalAqiRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        return try {
            // Initialize dependencies
            val database = AQIDatabase.getDatabase(applicationContext)
            localAqiRepository = LocalAqiRepository(database.aqiDao())
            aqiRepository = AQIRepository(applicationContext)
            userPreferencesManager = UserPreferencesManager(applicationContext)

            // Step 1: Get last known location
            val location = getLastLocation()
            if (location == null) {
                Log.e("AqiSyncWorker", "Failed to get location")
                return Result.retry()
            }

            val latitude = location.latitude
            val longitude = location.longitude

            // Step 2: Get city name from coordinates (via reverse geocoding)
            val locationResult = locationRepository.getLocationData(latitude.toString(), longitude.toString())
            if (locationResult.isFailure) {
                Log.e("AqiSyncWorker", "Location API failed: ${locationResult.exceptionOrNull()?.message}")
                return Result.retry()
            }
            val locationResponse = locationResult.getOrNull() ?: return Result.retry()
            val cityName = locationResponse.city

            // Step 3: Get weather data using city name
            val weatherResult = weatherRepository.getWeatherData(cityName)
            if (weatherResult.isFailure) {
                Log.e("AqiSyncWorker", "Weather API failed: ${weatherResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

            val weatherResponse = weatherResult.getOrNull() ?: return Result.retry()

            // Step 4: Predict AQI using the ML model
            val aqiResult = aqiRepository.predictAQI(weatherResponse)

            // Step 5: Create AQI object for saving
            val userId = userPreferencesManager.userId
            val date = LocalDate.now().toString()
            val timestamp = System.currentTimeMillis()
            val updatedAt = timestampToString(timestamp)

            val aqi = AQI(
                userId = userId,
                date = date,
                updatedAt = updatedAt,
                aqi = aqiResult.aqi,
                label = aqiResult.label,
                color = aqiResult.color,
                healthImpact = aqiResult.healthImpact,
                recommendedAction = aqiResult.recommendedAction
            )

            // Step 6: Save to remote Firebase
            saveWithCallback(aqi, date)

            // Step 7: Save to local database
            localAqiRepository.upsertAqi(aqi)

            // Step 8: Clean up
            aqiRepository.clear()

            Log.d("AqiSyncWorker", "AQI sync completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e("AqiSyncWorker", "Error during sync", e)
            Result.failure()
        }
    }

    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resume(null) { cause, _, _ -> onCancellation(cause) }
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location) { cause, _, _ -> onCancellation(cause) }
                }
                .addOnFailureListener {
                    continuation.resume(null) { cause, _, _ -> onCancellation(cause) }
                }
        }
    }

    private suspend fun saveWithCallback(aqi: AQI, date: String) {
        suspendCancellableCoroutine { continuation ->
            remoteAqiRepository.saveAQI(aqi, date) { result ->
                if (result.isSuccess) {
                    Log.d("AqiSyncWorker", "Saved to Firebase successfully")
                } else {
                    Log.e(
                        "AqiSyncWorker",
                        "Failed to save to Firebase: ${result.exceptionOrNull()?.message}"
                    )
                }
                continuation.resume(Unit) { cause, _, _ -> onCancellation(cause) }
            }
        }
    }

    private fun onCancellation(cause: Throwable) {
        Log.e("AqiSyncWorker", "Coroutine cancelled", cause)
    }
}

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val category = inputData.getString(WorkSchedulers.CATEGORY_KEY) ?: return Result.failure()
        val timeMillis = inputData.getLong(WorkSchedulers.NOTIFICATION_TIME_KEY, 0L)
        if (timeMillis == 0L) return Result.failure()

        return try {
            // Send broadcast
            val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
                putExtra("CATEGORY", category)
            }
            context.sendBroadcast(intent)

            // Schedule next day’s notification
            val zone = ZoneId.systemDefault()
            val nextDayTime = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDateTime().plusDays(1)
                    .atZone(zone).toInstant().toEpochMilli()
            WorkSchedulers.schedulePushNoti(context, nextDayTime, category)

            Log.d("WorkersDebug", "Broadcast of Category: $category, Time: ${Date(timeMillis)} sent to DailyNotificationReceiver")
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkersDebug", "Error sending notification broadcast: ${e.message}")
            Result.failure()
        }
    }
}
