package com.aqi.weather.sharedViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aqi.weather.data.local.database.AQIDatabase
import com.aqi.weather.data.local.database.entity.AQI
import com.aqi.weather.data.local.database.repository.LocalAqiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class LocalAqiViewModel (application: Application) : AndroidViewModel(application) {
    private val aqiDao = AQIDatabase.getDatabase(application).aqiDao()
    private val aqiRepo = LocalAqiRepository(aqiDao)

    fun saveAQI(aqi: AQI, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            aqiRepo.upsertAqi(aqi)
            // Allows callers to provide a callback function that should be executed after the aqi is successfully saved
            onSaved?.invoke()
        }
    }

    fun observeAQIs(userId: String): LiveData<List<AQI>> {
        val aqiData = MutableLiveData<List<AQI>>()
        viewModelScope.launch(Dispatchers.IO) {
            val aqis = aqiRepo.getAqiByUserId(userId)
            aqiData.postValue(aqis)
        }
        return aqiData
    }

    // Get last 7 days AQI data
    fun getLast7DaysAQI(userId: String): LiveData<List<AQI>> {
        val result = MutableLiveData<List<AQI>>()
        viewModelScope.launch(Dispatchers.IO) {
            val allAqis = aqiRepo.getAqiByUserId(userId)
            val last7Days = allAqis.takeLast(7)
            result.postValue(last7Days)
        }
        return result
    }

    // Get current month AQI data
    fun getCurrentMonthAQI(userId: String): LiveData<List<AQI>> {
        val result = MutableLiveData<List<AQI>>()
        viewModelScope.launch(Dispatchers.IO) {
            val allAqis = aqiRepo.getAqiByUserId(userId)
            val currentMonth = LocalDate.now().monthValue
            val currentYear = LocalDate.now().year

            val monthAqis = allAqis.filter { aqi ->
                val date = LocalDate.parse(aqi.date)
                date.monthValue == currentMonth && date.year == currentYear
            }
            result.postValue(monthAqis)
        }
        return result
    }

    suspend fun getAllAQIs(userId: String): List<AQI> {
        return aqiRepo.getAqiByUserId(userId)
    }

    fun getGroupedMyAQIs(userId: String): LiveData<Map<String, List<AQI>>> {
        val groupedData = MutableLiveData<Map<String, List<AQI>>>()
        viewModelScope.launch(Dispatchers.IO) {
            val aqis = aqiRepo.getAqiByUserId(userId)
            val grouped = aqis.groupBy { it.date }.toSortedMap(compareBy { it })
            groupedData.postValue(grouped)
        }
        return groupedData
    }
}