package com.aqi.weather.sharedViewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.remote.dto.LocationResponse
import com.aqi.weather.data.repos.LocationRepository
import com.aqi.weather.util.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationViewModel : ViewModel() {
    private val locationRepository = LocationRepository()

    private val _locationResponseState = MutableStateFlow<NetworkState<LocationResponse>>(NetworkState.Idle)
    val locationResponseState: StateFlow<NetworkState<LocationResponse>> = _locationResponseState.asStateFlow()

    suspend fun getLocationData(latitude: String, longitude: String) {
        _locationResponseState.value = NetworkState.Loading
        locationRepository.getLocationData(latitude, longitude)
            .onSuccess { response ->
                _locationResponseState.value = NetworkState.Success(response)
            }
            .onFailure { error ->
                _locationResponseState.value = NetworkState.Error(error.message ?: "Fetch location failed")
            }
    }

    fun resetState() {
        _locationResponseState.value = NetworkState.Idle
    }
}