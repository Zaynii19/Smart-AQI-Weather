package com.aqi.weather.data.local.database.repository

import com.aqi.weather.data.local.database.dao.AQIDao
import com.aqi.weather.data.local.database.entity.AQI

class LocalAqiRepository (private val aqiDao: AQIDao) {
    suspend fun upsertAqi(aqi: AQI) = aqiDao.upsertAqi(aqi)
    suspend fun getAqiByUserId(userId: String) = aqiDao.getAqiByUserId(userId)
    suspend fun getAqiByUserIdAndDate(userId: String, date: String) = aqiDao.getAqiByUserIdAndDate(userId, date)
}