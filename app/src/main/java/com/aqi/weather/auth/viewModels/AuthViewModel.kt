package com.aqi.weather.auth.viewModels

import androidx.lifecycle.ViewModel
import com.aqi.weather.data.repos.AuthRepository
import com.aqi.weather.util.NetworkState
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _firebaseAuthState = MutableStateFlow<NetworkState<String>>(NetworkState.Idle)
    val firebaseAuthState: StateFlow<NetworkState<String>> = _firebaseAuthState.asStateFlow()

    private val _signUpState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val signUpState: StateFlow<NetworkState<Boolean>> = _signUpState.asStateFlow()

    private val _signInState = MutableStateFlow<NetworkState<String>>(NetworkState.Idle)
    val signInState: StateFlow<NetworkState<String>> = _signInState.asStateFlow()

    private val _updateUserState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val updateUserState: StateFlow<NetworkState<Boolean>> = _updateUserState.asStateFlow()

    private val _deleteUserState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val deleteUserState: StateFlow<NetworkState<Boolean>> = _deleteUserState.asStateFlow()

    fun firebaseAuth(credential: AuthCredential, selectedUserType: String) {
        _firebaseAuthState.value = NetworkState.Loading

        authRepository.firebaseAuth(credential, selectedUserType) { result ->
            result.onSuccess { response ->
                _firebaseAuthState.value = NetworkState.Success(response)
            }.onFailure { error ->
                _firebaseAuthState.value = NetworkState.Error(error.message ?: "Firebase Auth failed")
            }
        }
    }

    fun signupUser(name: String, email: String, pass: String, selectedUserType: String) {
        _signUpState.value = NetworkState.Loading

        authRepository.signupUser(name, email, pass, selectedUserType) { result ->
            result.onSuccess { response ->
                _signUpState.value = NetworkState.Success(response)
            }.onFailure { error ->
                _signUpState.value = NetworkState.Error(error.message ?: "Signup failed")
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        _signInState.value = NetworkState.Loading

        authRepository.loginUser(email, pass) { result ->
            result.onSuccess { response ->
                _signInState.value = NetworkState.Success(response)
            }.onFailure { error ->
                _signInState.value = NetworkState.Error(error.message ?: "Login failed")
            }
        }
    }

    fun updateUser(updates: Map<String, Any?>, pathString: String) {
        _updateUserState.value = NetworkState.Loading

        authRepository.updateUser(updates, pathString) { result ->
            result.onSuccess { response ->
                _updateUserState.value = NetworkState.Success(response)
            }.onFailure { error ->
                _updateUserState.value = NetworkState.Error(error.message ?: "Update failed")
            }
        }
    }

    fun deleteUser(pathString: String) {
        _deleteUserState.value = NetworkState.Loading

        authRepository.deleteUser(pathString) { result ->
            result.onSuccess { response ->
                _deleteUserState.value = NetworkState.Success(response)
            }.onFailure { error ->
                _deleteUserState.value = NetworkState.Error(error.message ?: "Delete failed")
            }
        }
    }

    fun resetStates() {
        _firebaseAuthState.value = NetworkState.Idle
        _signUpState.value = NetworkState.Idle
        _signInState.value = NetworkState.Idle
        _updateUserState.value = NetworkState.Idle
        _deleteUserState.value = NetworkState.Idle
    }
}