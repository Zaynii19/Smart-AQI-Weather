package com.aqi.weather.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.R
import com.aqi.weather.admin.AdminMainActivity
import com.aqi.weather.auth.viewModels.AuthViewModel
import com.aqi.weather.citizen.CitizenMainActivity
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.ActivitySignInBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySignInBinding.inflate(layoutInflater)
    }
    private var selectedUserType: String = ""
    private var userEmail: String = ""
    private var userPass: String = ""
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
        )
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onUserSelection()
        observeSignInState()

        binding.signupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.signInBtn.setOnClickListener {
            userEmail = binding.email.text.toString()
            userPass = binding.pass.text.toString()
            when {
                userEmail.isEmpty() || userEmail.isBlank() ->  binding.email.error = "Email is required"
                userPass.isEmpty() || userPass.isBlank() -> binding.pass.error = "Password is required"
                !isInternetAvailable(this) -> showError("No internet available, please check your connection.")
                else -> {
                    authViewModel.loginUser(userEmail, userPass)
                }
            }
        }
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

    private fun observeSignInState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.signInState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleSignInSuccess(state.data)
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    // Handle user type comparison and navigation
    private fun handleSignInSuccess(actualUserType: String?) {
        binding.loading.visibility = View.GONE
        authViewModel.resetStates()
        if (actualUserType == selectedUserType) {
            Toast.makeText(this, "SignIn successful", Toast.LENGTH_SHORT).show()
            // Store user in SharedPreferences
            val firebaseUser = Firebase.auth.currentUser
            val userId = firebaseUser?.uid ?: return
            val prefsManager = UserPreferencesManager(this@SignInActivity)
            prefsManager.savePreference(userType = selectedUserType, userId = userId)

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
        Log.e("SignInDebug", "Error: $message")
        authViewModel.resetStates()
    }
}