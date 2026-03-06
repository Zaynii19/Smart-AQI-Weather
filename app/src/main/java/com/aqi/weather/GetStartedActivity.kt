package com.aqi.weather

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aqi.weather.auth.SignInActivity
import com.aqi.weather.databinding.ActivityGetStartedBinding

class GetStartedActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityGetStartedBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // For Dark status bar content
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE),
            //navigationBarStyle = SystemBarStyle.light(Color.Red.toArgb(), Color.White.toArgb())
            // For Light status bar content
            //statusBarStyle = SystemBarStyle.dark(Color.Black.toArgb()),
            //navigationBarStyle = SystemBarStyle.dark(Color.Black.toArgb())
        )
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        binding.getStarted.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}