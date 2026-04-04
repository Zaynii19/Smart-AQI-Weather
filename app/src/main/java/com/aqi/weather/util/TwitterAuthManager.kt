package com.aqi.weather.util

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider

class TwitterAuthManager(
    private val activity: AppCompatActivity,
    private val onSuccess: (AuthCredential) -> Unit,
    private val onError: (String) -> Unit
) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val provider = OAuthProvider.newBuilder("twitter.com")

    init {
        // Important: Check if there is a pending result (e.g. if activity was recreated)
        val pendingResultTask = firebaseAuth.pendingAuthResult
        pendingResultTask?.addOnSuccessListener { authResult ->
            authResult.credential?.let { onSuccess(it) }
        }?.addOnFailureListener { e ->
            onError(e.message ?: "Twitter pending auth failed")
        }
    }

    fun signInWithTwitter() {
        firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { authResult ->
                // Twitter is unique: it returns the Credential directly
                authResult.credential?.let { onSuccess(it) }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Twitter sign-in failed")
            }
    }
}