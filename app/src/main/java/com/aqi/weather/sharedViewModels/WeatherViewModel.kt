package com.aqi.weather.sharedViewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.remote.dto.WeatherResponse
import com.aqi.weather.data.repos.WeatherRepository
import com.aqi.weather.util.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherViewModel : ViewModel() {
    private val weatherRepository = WeatherRepository()

    private val _weatherResponseState = MutableStateFlow<NetworkState<WeatherResponse>>(NetworkState.Idle)
    val weatherResponseState: StateFlow<NetworkState<WeatherResponse>> = _weatherResponseState.asStateFlow()

    suspend fun getWeatherData(cityName: String) {
        _weatherResponseState.value = NetworkState.Loading
        weatherRepository.getWeatherData(cityName)
            .onSuccess { response ->
                _weatherResponseState.value = NetworkState.Success(response)
            }
            .onFailure { error ->
                _weatherResponseState.value = NetworkState.Error(error.message ?: "Fetch weather failed")
            }
    }

    fun resetState() {
        _weatherResponseState.value = NetworkState.Idle
    }
}