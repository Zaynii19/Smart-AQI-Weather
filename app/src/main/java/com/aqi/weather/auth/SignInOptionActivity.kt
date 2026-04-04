package com.aqi.weather.auth

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.R
import com.aqi.weather.admin.AdminMainActivity
import com.aqi.weather.citizen.CitizenMainActivity
import com.aqi.weather.databinding.ActivitySignInOptionBinding
import com.aqi.weather.util.FacebookAuthManager
import com.aqi.weather.util.GoogleAuthManager
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.TwitterAuthManager
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


class SignInOptionActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySignInOptionBinding.inflate(layoutInflater)
    }
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var userPreferences: SharedPreferences
    private var selectedUserType: String = "Citizen"
    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var facebookAuthManager: FacebookAuthManager
    private lateinit var twitterAuthManager: TwitterAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
        )
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userPreferences = getSharedPreferences("LOGIN", MODE_PRIVATE)

        onUserSelection()
        setupListeners()
        initializeGoogleAuth()
        initializeFacebookAuth()
        initializeTwitterAuth()
        observeFirebaseAuthState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the result to the Facebook utility
        facebookAuthManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun onUserSelection() {
        selectedUserType = binding.adminBtn.text.toString()
        binding.adminBtn.setOnClickListener {
            selectedUserType = binding.adminBtn.text.toString()
            binding.adminBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.citizenBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.adminBtn.setTextColor(Color.WHITE)
            binding.citizenBtn.setTextColor(getColor(R.color.dark_gray))
        }

        binding.citizenBtn.setOnClickListener {
            selectedUserType = binding.citizenBtn.text.toString()
            binding.citizenBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.adminBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.citizenBtn.setTextColor(Color.WHITE)
            binding.adminBtn.setTextColor(getColor(R.color.dark_gray))
        }
    }

    private fun setupListeners() {
        binding.google.setOnClickListener {
            if (isInternetAvailable(this)) {
                googleAuthManager.signInWithGoogle()
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        binding.facebook.setOnClickListener {
            if (isInternetAvailable(this)) {
                facebookAuthManager.signInWithFacebook()
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        binding.twitter.setOnClickListener {
            if (isInternetAvailable(this)) {
                twitterAuthManager.signInWithTwitter()
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        binding.email.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        binding.phone.setOnClickListener {
            // Handle Email Sign-In
        }
    }

    private fun initializeGoogleAuth() {
        googleAuthManager = GoogleAuthManager(
            activity = this,
            onSuccess = { accessToken ->
                val credential = GoogleAuthProvider.getCredential(accessToken, null)
                authViewModel.firebaseAuth(credential, selectedUserType)
            },
            onError = { error ->
                showError(error)
                Log.e("SignInOptionDebug", "Error: $error")
            }
        )
    }

    private fun initializeFacebookAuth() {
        facebookAuthManager = FacebookAuthManager(
            activity = this,
            onSuccess = { accessToken ->
                val credential = FacebookAuthProvider.getCredential(accessToken)
                authViewModel.firebaseAuth(credential, selectedUserType)
            },
            onError = { error ->
                showError(error)
                Log.e("SignInOptionDebug", "Error: $error")
            }
        )
    }

    private fun initializeTwitterAuth() {
        twitterAuthManager = TwitterAuthManager(
            activity = this,
            onSuccess = { credential ->
                authViewModel.firebaseAuth(credential, selectedUserType)
            },
            onError = { error ->
                showError(error)
                Log.e("SignInOptionDebug", "Twitter Error: $error")
            }
        )
    }

    private fun observeFirebaseAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.firebaseAuthState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleAuthSuccess(state.data)
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleAuthSuccess(actualUserType: String?) {
        binding.loading.visibility = View.GONE
        authViewModel.resetStates()
        if (actualUserType == selectedUserType) {
            Toast.makeText(this, "SignIn successful", Toast.LENGTH_SHORT).show()
            // Store user type in SharedPreferences
            userPreferences.edit { putString("USERTYPE", selectedUserType) }

            // Navigate to the correct dashboard
            when (selectedUserType) {
                "Admin" -> startActivity(Intent(this, AdminMainActivity::class.java))
                "Citizen" -> startActivity(Intent(this, CitizenMainActivity::class.java))
            }
            finish()
        } else {
            // Role mismatch, sign out and show error
            Firebase.auth.signOut()
            Toast.makeText(this, "Please SignIn in as the correct role.", Toast.LENGTH_SHORT).show()
            Log.d("LoginDebug", "Actual User: $actualUserType, SignIn as: $selectedUserType")
        }
    }

    private fun showError(message: String) {
        binding.loading.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("SignInOptionDebug", "Error: $message")
        authViewModel.resetStates()
    }
}