package com.aqi.weather.util

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aqi.weather.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Suppress("DEPRECATION")
class GoogleAuthManager(
    activity: AppCompatActivity,
    private val onSuccess: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val googleSignInClient: GoogleSignInClient
    private val webClientId = BuildConfig.WEB_CLIENT_ID

    private val googleLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    onSuccess(account.idToken ?: "")
                } catch (e: ApiException) {
                    onError(e.message ?: "Google sign-in failed")
                }
            }
        }

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            googleLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}