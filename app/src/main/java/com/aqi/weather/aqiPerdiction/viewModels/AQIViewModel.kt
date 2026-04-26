package com.aqi.weather.aqiPerdiction.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aqi.weather.aqiPerdiction.models.AQIResult
import com.aqi.weather.aqiPerdiction.repos.AQIRepository
import com.aqi.weather.data.remote.dto.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AQIViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AQIRepository(application)

    private val _aqiResult = MutableStateFlow<AQIResult?>(null)
    val aqiResult: StateFlow<AQIResult?> = _aqiResult

    fun fetchAQI(weather: WeatherResponse) {
        _aqiResult.value = null
        viewModelScope.launch {
            try {
                val result = repo.predictAQI(weather)
                _aqiResult.value = result
                Log.d("LocalAqiViewModel", "Result: $result")
            } catch (e: Exception) {
                Log.e("LocalAqiViewModel", "Prediction failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.clear()
    }
}