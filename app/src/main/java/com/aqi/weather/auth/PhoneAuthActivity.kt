package com.aqi.weather.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aqi.weather.R
import com.aqi.weather.databinding.ActivityPhoneAuthBinding

class PhoneAuthActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPhoneAuthBinding.inflate(layoutInflater)
    }
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

        val userType = intent.getStringExtra("userType")

        binding.phoneNum.requestFocus()
        binding.countryCodePicker.registerCarrierNumberEditText(binding.phoneNum)

        binding.continueBtn.setOnClickListener {
            if (!binding.countryCodePicker.isValidFullNumber) {
                binding.phoneNum.error = "Phone number not valid"
                return@setOnClickListener
            }
            val intent = Intent(this@PhoneAuthActivity, OTPActivity::class.java).apply {
                putExtra("userType", userType)
                putExtra("phoneNum", binding.countryCodePicker.fullNumberWithPlus)
            }
            startActivity(intent)
        }
    }
}