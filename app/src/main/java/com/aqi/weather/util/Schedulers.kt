package com.aqi.weather.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkSchedulers {
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

    fun scheduleNotifications(context: Context) {
        cancelNotificationsSchedule(context) // prevent duplicates

        val periodicRequest = PeriodicWorkRequestBuilder<NotificationWorker>(12, TimeUnit.HOURS)
            .addTag("AlertNotifications")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AlertNotifications",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        Log.d("WorkManagerDebug", "Scheduled AQI data sync every 12 hours")
    }

    fun cancelAqiDataSync(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("AqiDataSync")
        Log.d("WorkManagerDebug", "Canceled AQI data sync")
    }

    fun cancelNotificationsSchedule(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("AlertNotifications")
        Log.d("WorkManagerDebug", "Canceled notifications schedule")
    }
}