package com.aqi.weather

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aqi.weather.admin.AdminMainActivity
import com.aqi.weather.auth.SignInActivity
import com.aqi.weather.citizen.CitizenMainActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var userPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // User is signed in, determine role and redirect
        userPreferences = getSharedPreferences("LOGIN", MODE_PRIVATE)
        val userType = userPreferences.getString("USERTYPE", "")

        // Check if user is already signed in
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        lifecycleScope.launch {
            delay(3000) // Adjust this delay based on your requirements
            val intent = if (user != null) {
                when (userType) {
                    "Admin" -> {
                        // Redirect to admin home screen
                        Intent(this@SplashActivity, AdminMainActivity::class.java)
                    }
                    "Citizen" -> {
                        // Redirect to citizen home screen
                        Intent(this@SplashActivity, CitizenMainActivity::class.java)
                    }
                    else -> {
                        // User type not found, redirect to sign-in screen
                        Intent(this@SplashActivity, SignInActivity::class.java)
                    }
                }
            } else {
                // User is not signed in
                Intent(this@SplashActivity, SignInActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}