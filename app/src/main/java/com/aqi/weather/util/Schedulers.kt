package com.aqi.weather.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

object WorkSchedulers {
    const val NOTIFICATION_TIME_KEY = "NOTIFICATION_TIME"
    const val CATEGORY_KEY = "CATEGORY"

    fun scheduleAqiDataSync(context: Context) {
        cancelAqiDataSync(context) // prevent duplicates

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Use your actual AQI sync worker class here
        val periodicRequest = PeriodicWorkRequestBuilder<AqiSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("AqiDataSync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AqiDataSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        Log.d("WorkManagerDebug", "Scheduled AQI data sync every 12 hours")
    }

    // Schedule push notifications using WorkManager
    fun scheduleDailyNotifications(context: Context) {
        val categories = listOf(
            Triple("BEDTIME", 19, 0),      // 7:00 PM
            Triple("WAKEUP", 10, 0),       // 10:00 AM
            Triple("MOTIVATION", 17, 0)    // 5:00 PM
        )

        val zone = ZoneId.systemDefault()
        val now = Instant.now()

        for ((category, hour, minute) in categories) {
            var scheduledTime = LocalDate.now().atTime(hour, minute).atZone(zone).toInstant()

            // If the time has already passed today, move to tomorrow
            if (scheduledTime.isBefore(now)) {
                scheduledTime = LocalDate.now().plusDays(1).atTime(hour, minute).atZone(zone).toInstant()
            }

            schedulePushNoti(context, scheduledTime.toEpochMilli(), category)
        }
    }

    fun schedulePushNoti(context: Context, timeMillis: Long, category: String) {
        cancelPushNoti(context, category) // prevent duplicates

        val delay = timeMillis - System.currentTimeMillis()
        if (delay <= 0) return

        val data = workDataOf(
            NOTIFICATION_TIME_KEY to timeMillis,
            CATEGORY_KEY to category
        )

        // One-time
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("Notification_$category")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "Notification_$category",
            ExistingWorkPolicy.REPLACE,
            request
        )

        Log.d("WorkManagerDebug", "Notification of $category scheduled for ${Date(timeMillis)}")
    }

    fun cancelAqiDataSync(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("AqiDataSync")
        Log.d("WorkManagerDebug", "Canceled AQI data sync")
    }

    private fun cancelPushNoti(context: Context, category: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("Notification_$category")
        Log.d("WorkManagerDebug", "Canceled notification of $category")
    }
}