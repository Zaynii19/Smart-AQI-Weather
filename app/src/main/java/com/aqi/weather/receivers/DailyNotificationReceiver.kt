package com.aqi.weather.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aqi.weather.R
import com.aqi.weather.citizen.CitizenMainActivity
import com.aqi.weather.util.hasNotificationPermission

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!hasNotificationPermission(context)) return

        val title = intent.getStringExtra("TITLE") ?: "AQI Alert"
        val message = intent.getStringExtra("MESSAGE") ?: "Avoid outdoor exercise"

        createChannel(context)
        showNotification(context, title, message)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "AQI_ALERT", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "AQI Alert Channel"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.RED
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, CitizenMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.clouds)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.app_logo))
            .setColor(ContextCompat.getColor(context, R.color.buttons_color))
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Display longer text content than the standard notification
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(DAILY_NOTIFICATION_ID, notification)
            Log.d("DailyNotificationDebug", "Notification created: $title")
        } catch (e: Exception) {
            Log.e("DailyNotificationDebug", "Error showing notification", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_sleep_notifications"
        const val DAILY_NOTIFICATION_ID = 1001
    }
}