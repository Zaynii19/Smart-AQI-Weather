package com.aqi.weather.citizen.citizenViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aqi.weather.data.remote.dto.User
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

class CitizenViewModel : ViewModel() {
    private val _citizensState = MutableStateFlow<NetworkState<List<User>>>(NetworkState.Idle)
    val citizensState: StateFlow<NetworkState<List<User>>> = _citizensState.asStateFlow()
    private val _citizenDataState = MutableStateFlow<NetworkState<User>>(NetworkState.Idle)
    val citizenDataState: StateFlow<NetworkState<User>> = _citizenDataState.asStateFlow()

    // Keep references to listener for cleanup
    private var citizensListener: ValueEventListener? = null
    private var citizenDataListener: ValueEventListener? = null

    fun retrieveCitizens() {
        _citizensState.value = NetworkState.Loading

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = mutableListOf<User>()
                for (child in snapshot.children) {
                    val citizenItem = child.getValue(User::class.java)
                    if (citizenItem != null) {
                        itemList.add(citizenItem)
                    }
                }
                _citizensState.value = if (itemList.isEmpty()) {
                    NetworkState.Success(emptyList())
                } else {
                    NetworkState.Success(itemList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CitizenVMDebug", "Failed to retrieve citizen", error.toException())
                _citizensState.value = NetworkState.Error(error.message)
            }
        }

        citizensListener = listener
        Firebase.database.reference.child("Citizen").addValueEventListener(listener)
    }

    fun retrieveCitizenData() {
        _citizenDataState.value = NetworkState.Loading

        // Check if user is logged in
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            _citizenDataState.value = NetworkState.Error("User not authenticated")
            return
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val citizen = snapshot.getValue<User>()

                _citizenDataState.value = if (citizen == null) {
                    NetworkState.Success(User())
                } else {
                    NetworkState.Success(citizen)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CitizenVMDebug", "Failed to retrieve citizen", error.toException())
                _citizenDataState.value = NetworkState.Error(error.message)
            }
        }

        citizenDataListener = listener
        Firebase.database.reference.child("Citizen").child(currentUser.uid)
            .addValueEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listeners to prevent memory leaks
        citizensListener?.let {
            Firebase.database.reference.child("Citizen").removeEventListener(it)
        }

        Firebase.auth.currentUser?.let { user ->
            citizenDataListener?.let { listener ->
                Firebase.database.reference.child("Citizen").child(user.uid)
                    .removeEventListener(listener)
            }
        }

        // Clear references
        citizensListener = null
        citizenDataListener = null
    }
}