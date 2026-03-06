package com.aqi.weather.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.aqi.weather.citizen.citizenViewModels.CitizenViewModel
import com.aqi.weather.R
import com.aqi.weather.auth.SignInActivity
import com.aqi.weather.citizen.citizenAdapters.CitizenAdapter
import com.aqi.weather.auth.UserModel
import com.aqi.weather.databinding.ActivityAdminMainBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class AdminMainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAdminMainBinding.inflate(layoutInflater)
    }
    private val viewModel: CitizenViewModel by viewModels()

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

        viewModel.retrieveCitizens()
        observeCitizenState()

        binding.signOutBtn.setOnClickListener {
            if (isInternetAvailable(this)) {
                Firebase.auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeCitizenState() {
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

    private fun showCitizens(citizens: List<UserModel>) {
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
    }
}