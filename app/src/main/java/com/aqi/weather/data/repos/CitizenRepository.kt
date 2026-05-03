package com.aqi.weather.data.repos

import android.util.Log
import com.aqi.weather.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class CitizenRepository {

    fun retrieveCitizens(onResult: (Result<List<User>>) -> Unit) {
        val citizenRef = Firebase.database.reference.child("Citizen")

        citizenRef.get().addOnSuccessListener { snapshot ->
            val itemList = mutableListOf<User>()
            for (child in snapshot.children) {
                val citizenItem = child.getValue(User::class.java)
                if (citizenItem != null) {
                    itemList.add(citizenItem)
                }
            }
            onResult(Result.success(itemList))
        }.addOnFailureListener { exception ->
            Log.e("CitizenRepoDebug", "Failed to retrieve citizen", exception)
            onResult(Result.failure(exception))
        }
    }

    fun retrieveCitizenData(onResult: (Result<User>) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val citizenRef = Firebase.database.reference.child("Citizen").child(currentUser.uid)

        citizenRef.get().addOnSuccessListener { snapshot ->
            val citizen = snapshot.getValue(User::class.java)
            val result = if (citizen == null) {
                Result.success(User())
            } else {
                Result.success(citizen)
            }
            onResult(result)
        }.addOnFailureListener { exception ->
            Log.e("CitizenRepoDebug", "Failed to retrieve citizen data", exception)
            onResult(Result.failure(exception))
        }
    }
}