package com.aqi.weather.aqiPerdiction

import com.aqi.weather.aqiPerdiction.models.TimeFeatures
import com.aqi.weather.data.remote.dto.WeatherResponse
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

object FeatureEngineer {
    fun calculateDewPoint(temp: Double, humidity: Double): Double {
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temp) / (b + temp)) + ln(humidity / 100.0)
        return (b * alpha) / (a - alpha)
    }

    fun generateTimeFeatures(timestamp: Long): TimeFeatures {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp * 1000
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        return TimeFeatures(
            hourSin = sin(2 * Math.PI * hour / 24),
            hourCos = cos(2 * Math.PI * hour / 24),
            doySin = sin(2 * Math.PI * dayOfYear / 365),
            doyCos = cos(2 * Math.PI * dayOfYear / 365)
        )
    }

    fun buildFeatures(weather: WeatherResponse): FloatArray {

        val temp = weather.main.temp
        val humidity = weather.main.humidity.toDouble()
        val pressure = weather.main.pressure.toDouble()
        val windSpeed = weather.wind.speed
        val windDeg = weather.wind.deg.toDouble()
        val lat = weather.coord.lat
        val lon = weather.coord.lon
        val dewPoint = calculateDewPoint(temp, humidity)
        val precipitation = 0.0
        val radiation = estimateRadiation(
            weather.dt.toLong(),
            weather.sys.sunrise.toLong(),
            weather.sys.sunset.toLong()
        )
        val time = generateTimeFeatures(weather.dt.toLong())

        return floatArrayOf(
            temp.toFloat(),
            humidity.toFloat(),
            dewPoint.toFloat(),
            precipitation.toFloat(),
            pressure.toFloat(),
            windSpeed.toFloat(),
            windDeg.toFloat(),
            radiation.toFloat(),
            lat.toFloat(),
            lon.toFloat(),
            time.hourSin.toFloat(),
            time.hourCos.toFloat(),
            time.doySin.toFloat(),
            time.doyCos.toFloat()
        )
    }

    fun estimateRadiation(current: Long, sunrise: Long, sunset: Long): Double {
        return if (current in sunrise..sunset) 300.0 else 0.0
    }
}