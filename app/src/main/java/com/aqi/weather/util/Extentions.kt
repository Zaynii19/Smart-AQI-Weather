package com.aqi.weather.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.aqi.weather.data.local.database.entity.AQI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED


fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

fun timestampToString(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
}

fun getTimeAgoFromTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    return try {
        val dateTimeUtc = Instant.ofEpochMilli(timestamp)
        val now = Instant.now()

        val duration = Duration.between(dateTimeUtc, now)
        val seconds = duration.seconds

        when {
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60} min ago"
            seconds < 86400 -> "${seconds / 3600} hr ago"
            else -> "${seconds / 86400} days ago"
        }
    } catch (_: Exception) {
        ""
    }
}

fun getCircularAqiDrawable(context: Context, aqi: AQI): Drawable {
    val size = 100 // Total diameter of the circle in pixels
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    // 1. Setup Background Circle Paint
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = aqi.color // Uses the color from your AQI object
        style = Paint.Style.FILL
    }

    // 2. Draw the Circle
    val center = size / 2f
    val radius = size / 2f
    canvas.drawCircle(center, center, radius, backgroundPaint)

    // 3. Setup Text Paint
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // 4. Calculate Vertical Center for Text
    // Text is drawn from the baseline, so we adjust to center it vertically
    val text = aqi.aqi.toString()
    val textHeight = textPaint.descent() - textPaint.ascent()
    val textOffset = textHeight / 2 - textPaint.descent()

    canvas.drawText(text, center, center + textOffset, textPaint)

    return bitmap.toDrawable(context.resources)
}

fun getDayString(dateString: String): String {
    return try {
        val inputDate = LocalDate.parse(dateString)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        when (inputDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> inputDate.format(DateTimeFormatter.ofPattern("EEEE")) // Returns Monday, Tuesday, etc.
        }
    } catch (_: DateTimeParseException) {
        // Handle different date format if needed
        "Invalid date"
    }
}

sealed class NetworkState<out T> {
    object Idle : NetworkState<Nothing>()
    object Loading : NetworkState<Nothing>()
    data class Success<T>(val data: T) : NetworkState<T>()
    data class Error(val message: String) : NetworkState<Nothing>()
}