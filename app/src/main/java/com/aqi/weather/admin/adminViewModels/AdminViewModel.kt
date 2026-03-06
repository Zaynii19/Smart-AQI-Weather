package com.aqi.weather.admin.adminViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aqi.weather.auth.UserModel
import com.aqi.weather.util.NetworkState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {
    private val _adminState = MutableStateFlow<NetworkState<List<UserModel>>>(NetworkState.Idle)
    val adminState: StateFlow<NetworkState<List<UserModel>>> = _adminState.asStateFlow()
    private val _adminDataState = MutableStateFlow<NetworkState<UserModel>>(NetworkState.Idle)
    val adminDataState: StateFlow<NetworkState<UserModel>> = _adminDataState.asStateFlow()

    // Keep references to listener for cleanup
    private var adminListener: ValueEventListener? = null
    private var adminDataListener: ValueEventListener? = null

    fun retrieveAdmins() {
        _adminState.value = NetworkState.Loading

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = mutableListOf<UserModel>()
                for (child in snapshot.children) {
                    val adminItem = child.getValue(UserModel::class.java)
                    if (adminItem != null) {
                        itemList.add(adminItem)
                    }
                }
                _adminState.value = if (itemList.isEmpty()) {
                    NetworkState.Success(emptyList())
                } else {
                    NetworkState.Success(itemList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminVMDebug", "Failed to retrieve admin", error.toException())
                _adminState.value = NetworkState.Error(error.message)
            }
        }

        adminListener = listener
        Firebase.database.reference.child("Admin").addValueEventListener(listener)
    }

    fun retrieveAdminData() {
        _adminDataState.value = NetworkState.Loading

        // Check if user is logged in
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            _adminDataState.value = NetworkState.Error("User not authenticated")
            return
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val admin = snapshot.getValue<UserModel>()

                _adminDataState.value = if (admin == null) {
                    NetworkState.Success(UserModel())
                } else {
                    NetworkState.Success(admin)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminVMDebug", "Failed to retrieve admin", error.toException())
                _adminDataState.value = NetworkState.Error(error.message)
            }
        }

        adminDataListener = listener
        Firebase.database.reference.child("Admin").child(currentUser.uid)
            .addValueEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listeners to prevent memory leaks
        adminListener?.let {
            Firebase.database.reference.child("Admin").removeEventListener(it)
        }

        Firebase.auth.currentUser?.let { user ->
            adminDataListener?.let { listener ->
                Firebase.database.reference.child("Admin").child(user.uid)
                    .removeEventListener(listener)
            }
        }

        // Clear references
        adminListener = null
        adminDataListener = null
    }
}