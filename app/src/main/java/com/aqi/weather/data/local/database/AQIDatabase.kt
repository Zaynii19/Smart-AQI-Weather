package com.aqi.weather.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aqi.weather.data.local.database.dao.AQIDao
import com.aqi.weather.data.local.database.entity.AQI

@Database(
    entities = [ AQI::class ],
    version = 1,
    exportSchema = false
)

abstract class AQIDatabase : RoomDatabase() {

    abstract fun aqiDao(): AQIDao

    companion object {
        @Volatile
        private var INSTANCE: AQIDatabase? = null

        fun getDatabase(context: Context): AQIDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AQIDatabase::class.java,
                    "aqi_database"
                ).fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}