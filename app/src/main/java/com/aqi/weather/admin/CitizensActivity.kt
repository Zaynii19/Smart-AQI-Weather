package com.aqi.weather.admin

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
import com.aqi.weather.R
import com.aqi.weather.auth.viewModels.AuthViewModel
import com.aqi.weather.citizen.adapters.CitizenAdapter
import com.aqi.weather.citizen.viewModels.CitizenViewModel
import com.aqi.weather.data.model.User
import com.aqi.weather.databinding.ActivityCitizensBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.isInternetAvailable
import kotlinx.coroutines.launch

class CitizensActivity : AppCompatActivity(), CitizenAdapter.OnItemActionListener {
    private val binding by lazy {
        ActivityCitizensBinding.inflate(layoutInflater)
    }
    private val citizenViewModel: CitizenViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

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

        citizenViewModel.retrieveCitizens()
        observeCitizenState()
        observeDeleteState()
    }

    private fun observeCitizenState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                citizenViewModel.citizenState.collect { state ->
                    when {
                        !isInternetAvailable(this@CitizensActivity) -> showError(
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

    private fun observeDeleteState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.deleteUserDataState.collect { state ->
                    when {
                        !isInternetAvailable(this@CitizensActivity) -> showError(
                            "No internet available, please check your connection."
                        )

                        state is NetworkState.Loading -> showLoading()
                        state is NetworkState.Success -> {
                            // Refresh UI
                            citizenViewModel.retrieveCitizens()
                            Toast.makeText(
                                this@CitizensActivity,
                                "User Deleted Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.loading.visibility = View.GONE
                            binding.errorText.visibility = View.GONE
                            authViewModel.resetStates()
                        }
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
            binding.rcv.adapter = CitizenAdapter(citizens, this)
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

    override fun onDelete(citizen: User) {
        authViewModel.deleteUserDate("Citizen", citizen.id)
    }
}