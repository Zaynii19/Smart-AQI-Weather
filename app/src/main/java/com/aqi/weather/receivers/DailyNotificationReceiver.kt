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
import com.aqi.weather.admin.AdminMainActivity
import java.util.Calendar

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val category = intent.getStringExtra("CATEGORY") ?: return

        val (title, message) = getNotificationContent(category)
        createChannel(context)
        //showNotification(context, title, message, category.hashCode())
        showNotification(context, title, message)
    }

    private fun getNotificationContent(category: String): Pair<String, String> {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val index = when (day) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }

        val bedtimeTitles = listOf(
            "Almost Bedtime 🌙",
            "Countdown to Dreamland ✨",
            "Recharge Mode 🌌",
            "Quiet the Mind 🕊",
            "Peaceful Night Ahead 😴",
            "Night Reset 🌠",
            "Unwind & Rest 🌃"
        )

        val bedtimeMsgs = listOf(
            "Let’s wind down for a fresh tomorrow. Relax your mind and prepare for peaceful sleep.",
            "Time to unplug, breathe deep, and slow down.",
            "It’s time to rest. Let your body and mind recharge for a productive tomorrow.",
            "Quiet thoughts, calm breath — drift gently into rest.",
            "Get cozy and unwind. Prepare your space for relaxation and calm.",
            "Your best tomorrow starts with a good night’s sleep tonight. Sweet dreams.",
            "Reflect, relax, and reset. Let go of the day and welcome peace."
        )

        val wakeTitles = listOf(
            "Rise & Grind 💪",
            "Bright Start 🌞",
            "New Day, New Goals 🌅",
            "Morning Momentum ⚡",
            "Fresh Start ☕",
            "Positive Vibes 🌤",
            "Awaken & Shine ✨"
        )

        val wakeMsgs = listOf(
            "Start your day strong — log your wake-up time and set your intentions.",
            "Morning sunshine! Stay consistent and embrace today’s opportunities.",
            "You’re up and active — keep your energy focused and your goals clear.",
            "Own your morning and set the tone for a powerful day ahead.",
            "Keep your streak alive and welcome the day with focus and gratitude.",
            "Good morning! A calm mind and clear plan lead to a great day.",
            "Rise with purpose and bring positive energy into your morning."
        )

        val motivationTitles = listOf(
            "Better Sleep, Better You 💤",
            "Healing Through Rest 🌙",
            "Strength in Sleep ✨",
            "Recovery Mode 😌",
            "Sharper Mind 🧠",
            "Immunity Boost ❤",
            "Power Up for Tomorrow ⚡"
        )

        val motivationMsgs = listOf(
            "Every night of good sleep builds a better, stronger you. Stay consistent!",
            "Sleep heals your body, balances your mood, and fuels your energy. Prioritize it tonight.",
            "Strong minds and bodies are built through consistent rest — keep your rhythm steady.",
            "Rest isn’t lazy — it’s how you recharge to stay sharp and energized.",
            "Sleep strengthens memory, focus, and creativity. Make it part of your success routine.",
            "Good sleep fuels strong immunity. Take care of your body with proper rest.",
            "Energy for tomorrow starts with balance and restful sleep tonight. Recharge fully."
        )

        return when (category) {
            "BEDTIME" -> bedtimeTitles[index] to bedtimeMsgs[index]
            "WAKEUP" -> wakeTitles[index] to wakeMsgs[index]
            "MOTIVATION" -> motivationTitles[index] to motivationMsgs[index]
            else -> "Sleep Reminder" to "Maintain your healthy sleep schedule!"
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Daily Sleep Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Daily notifications for sleep tracking"
            enableVibration(true)
            enableLights(true)
            lightColor = Color.RED
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, AdminMainActivity::class.java)
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
            Log.d("DailyNoti", "Notification created: $title")
        } catch (e: Exception) {
            Log.e("DailyNoti", "Error showing notification", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_sleep_notifications"
        const val DAILY_NOTIFICATION_ID = 1001
    }
}
