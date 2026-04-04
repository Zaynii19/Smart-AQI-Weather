package com.aqi.weather.admin

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
import com.aqi.weather.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAdminMainBinding.inflate(layoutInflater)
    }
    private lateinit var navController: NavController
    //private val viewModel: CitizenViewModel by viewModels()

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

    private fun setBottomNav() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentView) as NavHostFragment
        navController = navHostFragment.navController
        binding.navBottom.setupWithNavController(navController)
    }

   /* private fun observeCitizenState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.citizensState.collect { state ->
                    when {
                        !isInternetAvailable(this@AdminMainActivity) -> showError(
                            "No internet available, please check your connection."
                        )
                        state is NetworkState.Loading -> showLoading()
                        state is NetworkState.Success -> showCitizens(state.data)
                        state is NetworkState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.rcv.visibility = View.GONE
        binding.noDataAnim.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
    }

    private fun showCitizens(citizens: List<User>) {
        if (citizens.isEmpty()) {
            binding.rcv.visibility = View.GONE
            binding.noDataAnim.visibility = View.VISIBLE
            binding.loading.visibility = View.GONE
            binding.errorText.visibility = View.GONE
        } else {
            binding.rcv.visibility = View.VISIBLE
            binding.rcv.adapter = CitizenAdapter(citizens)
            binding.noDataAnim.visibility = View.GONE
            binding.loading.visibility = View.GONE
            binding.errorText.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.rcv.visibility = View.GONE
        binding.noDataAnim.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }*/
}