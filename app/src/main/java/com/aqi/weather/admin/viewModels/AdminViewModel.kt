package com.aqi.weather.admin.viewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.model.User
import com.aqi.weather.data.repos.AdminRepository
import com.aqi.weather.util.NetworkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {
    private val adminRepository = AdminRepository()

    private val _adminState = MutableStateFlow<NetworkState<List<User>>>(NetworkState.Idle)
    val adminState: StateFlow<NetworkState<List<User>>> = _adminState.asStateFlow()

    private val _adminDataState = MutableStateFlow<NetworkState<User>>(NetworkState.Idle)
    val adminDataState: StateFlow<NetworkState<User>> = _adminDataState.asStateFlow()

    fun retrieveAdmins() {
        _adminState.value = NetworkState.Loading

        adminRepository.retrieveAdmins { result ->
            result.onSuccess { users ->
                _adminState.value = NetworkState.Success(users)
            }.onFailure { error ->
                _adminState.value = NetworkState.Error(error.message ?: "Failed to retrieve admins")
            }
        }
    }

    fun retrieveAdminData() {
        _adminState.value = NetworkState.Loading

        adminRepository.retrieveAdminData { result ->
            result.onSuccess { user ->
                _adminDataState.value = NetworkState.Success(user)
            }.onFailure { error ->
                _adminDataState.value = NetworkState.Error(error.message ?: "Failed to retrieve admin data")
            }
        }
    }

    fun resetStates() {
        _adminState.value = NetworkState.Idle
        _adminDataState.value = NetworkState.Idle
    }
}