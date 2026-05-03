package com.aqi.weather.citizen.viewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.model.User
import com.aqi.weather.data.repos.CitizenRepository
import com.aqi.weather.util.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CitizenViewModel : ViewModel() {
    private val citizenRepository = CitizenRepository()

    private val _citizenState = MutableStateFlow<NetworkState<List<User>>>(NetworkState.Idle)
    val citizenState: StateFlow<NetworkState<List<User>>> = _citizenState.asStateFlow()

    private val _citizenDataState = MutableStateFlow<NetworkState<User>>(NetworkState.Idle)
    val citizenDataState: StateFlow<NetworkState<User>> = _citizenDataState.asStateFlow()

    fun retrieveCitizens() {
        _citizenState.value = NetworkState.Loading

        citizenRepository.retrieveCitizens { result ->
            result.onSuccess { users ->
                _citizenState.value = NetworkState.Success(users)
            }.onFailure { error ->
                _citizenState.value = NetworkState.Error(error.message ?: "Failed to retrieve citizens")
            }
        }
    }

    fun retrieveCitizenData() {
        _citizenState.value = NetworkState.Loading

        citizenRepository.retrieveCitizenData { result ->
            result.onSuccess { user ->
                _citizenDataState.value = NetworkState.Success(user)
            }.onFailure { error ->
                _citizenDataState.value = NetworkState.Error(error.message ?: "Failed to retrieve citizen data")
            }
        }
    }

    fun resetStates() {
        _citizenState.value = NetworkState.Idle
        _citizenDataState.value = NetworkState.Idle
    }
}