package com.aqi.weather.data.repos

import android.util.Log
import com.aqi.weather.data.local.database.entity.AQI
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class RemoteAqiRepository {
    fun saveAQI(
        aqi: AQI,
        date: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val firebaseUser = Firebase.auth.currentUser
        val userId = firebaseUser?.uid ?: run {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val aqiRef = Firebase.database.reference.child("AQI").child(userId).child(date)

        aqiRef.setValue(aqi).addOnCompleteListener { storeTask ->
            if (!storeTask.isSuccessful) {
                Log.e(
                    "AQIRepoDebug",
                    "Failed to save AQI data: ${storeTask.exception?.localizedMessage}"
                )
            }
        }
    }

    fun retrieveAqiData(onResult: (Result<List<AQI>>) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val aqiRef = Firebase.database.reference.child("AQI").child(currentUser.uid)

        aqiRef.get().addOnSuccessListener { snapshot ->
            val itemList = mutableListOf<AQI>()
            for (child in snapshot.children) {
                val aqiItem = child.getValue(AQI::class.java)
                if (aqiItem != null) {
                    itemList.add(aqiItem)
                }
            }
            onResult(Result.success(itemList))
        }.addOnFailureListener { exception ->
            Log.e("AdminRepoDebug", "Failed to retrieve Aqi Data", exception)
            onResult(Result.failure(exception))
        }
    }

    fun updateAQI(
        updates: Map<String, Any?>,
        date: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val firebaseUser = Firebase.auth.currentUser
        val userId = firebaseUser?.uid ?: run {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val aqiRef = Firebase.database.reference.child("AQI").child(userId).child(date)

        aqiRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("AQIRepoDebug", "AQI data updated successfully")
                onResult(Result.success(true))
            }
            .addOnFailureListener { dbException ->
                Log.e("AQIRepoDebug", "Error updating AQI data", dbException)
                onResult(Result.failure(Exception("Failed to update AQI data: ${dbException.localizedMessage}")))
            }
    }

    fun deleteAQI(
        date: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val firebaseUser = Firebase.auth.currentUser
        val userId = firebaseUser?.uid ?: run {
            onResult(Result.failure(Exception("User not authenticated")))
            return
        }

        val aqiRef = FirebaseDatabase.getInstance().getReference("AQI").child(userId).child(date)

        aqiRef.removeValue().addOnCompleteListener { dbTask ->
            if (dbTask.isSuccessful) {
                Log.d("AQIRepoDebug", "AQI data deleted successfully.")
                onResult(Result.success(true))
            } else {
                Log.e("AQIRepoDebug", "Database deletion failed", dbTask.exception)
                onResult(
                    Result.failure(
                        dbTask.exception ?: Exception("Failed to delete AQI data")
                    )
                )
            }
        }
    }
}