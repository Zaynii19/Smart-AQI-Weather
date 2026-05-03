package com.aqi.weather.citizen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aqi.weather.R
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.ActivityCitizenMainBinding
import com.aqi.weather.util.WorkSchedulers
import com.aqi.weather.util.hasNotificationPermission

class CitizenMainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityCitizenMainBinding.inflate(layoutInflater)
    }
    private lateinit var navController: NavController

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

        setBottomNav()
    }

    override fun onResume() {
        super.onResume()
        WorkSchedulers.cancelAqiDataSync(this)
        val prefsManager = UserPreferencesManager(this)
        val notificationEnabled = prefsManager.notificationEnabled

        if (notificationEnabled && hasNotificationPermission(this)) {
            WorkSchedulers.scheduleNotifications(this)
        } else {
            WorkSchedulers.cancelNotificationsSchedule(this)
        }
    }

    override fun onPause() {
        super.onPause()
        WorkSchedulers.scheduleAqiDataSync(this)
    }

    private fun setBottomNav() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentView) as NavHostFragment
        navController = navHostFragment.navController
        binding.navBottom.setupWithNavController(navController)
    }
}