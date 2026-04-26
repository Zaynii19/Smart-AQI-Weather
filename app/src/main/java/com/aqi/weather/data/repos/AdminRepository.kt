package com.aqi.weather.data.repos

import android.util.Log
import com.aqi.weather.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class AdminRepository {

    fun retrieveAdmins(onResult: (Result<List<User>>) -> Unit) {
        val adminRef = Firebase.database.reference.child("Admin")

        adminRef.get().addOnSuccessListener { snapshot ->
            val itemList = mutableListOf<User>()
            for (child in snapshot.children) {
                val adminItem = child.getValue(User::class.java)
                if (adminItem != null) {
                    itemList.add(adminItem)
                }
            }
            onResult(Result.success(itemList))
        }.addOnFailureListener { exception ->
            Log.e("AdminRepoDebug", "Failed to retrieve admin", exception)
            onResult(Result.failure(exception))
        }
    }

    fun retrieveAdminData(onResult: (Result<User>) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val adminRef = Firebase.database.reference.child("Admin").child(currentUser.uid)

        adminRef.get().addOnSuccessListener { snapshot ->
            val admin = snapshot.getValue(User::class.java)
            val result = if (admin == null) {
                Result.success(User())
            } else {
                Result.success(admin)
            }
            onResult(result)
        }.addOnFailureListener { exception ->
            Log.e("AdminRepoDebug", "Failed to retrieve admin data", exception)
            onResult(Result.failure(exception))
        }
    }
}