package com.aqi.weather.sharedViewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.local.database.entity.AQI
import com.aqi.weather.data.repos.RemoteAqiRepository
import com.aqi.weather.util.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class RemoteAqiViewModel : ViewModel() {
    private val aqiRepository = RemoteAqiRepository()

    private val _aqiSaveState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val aqiSaveState: StateFlow<NetworkState<Boolean>> = _aqiSaveState.asStateFlow()
    private val _aqiDataState = MutableStateFlow<NetworkState<List<AQI>>>(NetworkState.Idle)
    val aqiDataState: StateFlow<NetworkState<List<AQI>>> = _aqiDataState.asStateFlow()

    fun saveAqiData(aqi: AQI, date: String) {
        _aqiSaveState.value = NetworkState.Loading

        aqiRepository.saveAQI(aqi, date) { result ->
            result.onSuccess { success ->
                _aqiSaveState.value = NetworkState.Success(success)
            }.onFailure { error ->
                _aqiSaveState.value = NetworkState.Error(error.message ?: "Failed to save AQI data")
            }
        }
    }

    fun retrieveAqiData() {
        _aqiDataState.value = NetworkState.Loading

        aqiRepository.retrieveAqiData { result ->
            result.onSuccess { aqi ->
                _aqiDataState.value = NetworkState.Success(aqi)
            }.onFailure { error ->
                _aqiDataState.value = NetworkState.Error(error.message ?: "Failed to retrieve aqi data")
            }
        }
    }

    fun resetStates() {
        _aqiSaveState.value = NetworkState.Idle
        _aqiDataState.value = NetworkState.Idle
    }
}