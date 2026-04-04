package com.aqi.weather.util

import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import androidx.appcompat.app.AppCompatActivity

class FacebookAuthManager(
    private val activity: AppCompatActivity,
    private val onSuccess: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val callbackManager = CallbackManager.Factory.create()

    init {
        // Register the callback with the LoginManager
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    // This is the token Firebase needs
                    val token = result.accessToken.token
                    onSuccess(token)
                }

                override fun onCancel() {
                    onError("Facebook login cancelled")
                }

                override fun onError(error: FacebookException) {
                    onError(error.message ?: "Facebook login failed")
                }
            }
        )
    }

    fun signInWithFacebook() {
        // Request public_profile and email permissions
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            listOf("public_profile", "email")
        )
    }

    // Crucial: Facebook needs to receive the activity results
    // This isn't needed if using the latest SDK with certain configs,
    // but remains the standard for custom utility classes.
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}