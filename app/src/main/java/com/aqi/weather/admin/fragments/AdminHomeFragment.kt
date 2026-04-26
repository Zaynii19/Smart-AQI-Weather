package com.aqi.weather.admin.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.R
import com.aqi.weather.aqiPerdiction.models.AQIResult
import com.aqi.weather.aqiPerdiction.viewModels.AQIViewModel
import com.aqi.weather.data.local.database.entity.AQI
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.data.remote.dto.LocationResponse
import com.aqi.weather.data.remote.dto.WeatherResponse
import com.aqi.weather.databinding.FragmentAdminHomeBinding
import com.aqi.weather.sharedViewModels.LocalAqiViewModel
import com.aqi.weather.sharedViewModels.LocationViewModel
import com.aqi.weather.sharedViewModels.RemoteAqiViewModel
import com.aqi.weather.sharedViewModels.WeatherViewModel
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.getTimeAgoFromTimestamp
import com.aqi.weather.util.isInternetAvailable
import com.aqi.weather.util.openAppSettings
import com.aqi.weather.util.timestampToString
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class AdminHomeFragment : Fragment() {
    private val binding by lazy {
        FragmentAdminHomeBinding.inflate(layoutInflater)
    }
    private val locationViewModel: LocationViewModel by viewModels()
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val aqiViewModel: AQIViewModel by viewModels()
    private val localAqiViewModel: LocalAqiViewModel by viewModels()
    private val remoteAqiViewModel: RemoteAqiViewModel by viewModels()
    private var cityName = "Islamabad"
    private var updatedAt = System.currentTimeMillis()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                getLastLocation() // Call your function to get location
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                getLastLocation() // Call your function to get location
            }
            else -> {
                handlePermissionDenied()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (isInternetAvailable(requireContext())) {
            checkLocationPermission()
        } else {
            Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLocationState()
        observeWeatherState()
        observeAQIResult()
        setupSwipeRefresh()
        setSearchView()
        startRefreshLoop()

        binding.locationBtn.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                checkLocationPermission()
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.blue)
        binding.swipeRefresh.setOnRefreshListener {
            if (isInternetAvailable(requireContext())) {
                // Refresh data here
                lifecycleScope.launch {
                    weatherViewModel.getWeatherData(cityName)
                }
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
            binding.swipeRefresh.isRefreshing = false // Hide the refresh animation after data is fetched
        }
    }

    private fun setSearchView() {
        // Change text color to white of search view
        binding.searchLocation.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextColor(Color.WHITE)
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.very_light_gray))
        }

        // Get app color from colors.xml
        binding.searchLocation.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            ?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))

        binding.searchLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (isInternetAvailable(requireContext())) {
                    query?.let { cityName ->
                        if (cityName.isNotBlank()) {
                            this@AdminHomeFragment.cityName = cityName
                            lifecycleScope.launch {
                                weatherViewModel.getWeatherData(cityName)
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
                }
                binding.searchLocation.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                getLastLocation()
            }

            else -> {
                // Permission not granted, request it
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun handlePermissionDenied() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // User denied before, show explanation
            showLocationRationaleDialog()
        } else {
            // User permanently denied or first time with "Don't ask again"
            Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show()
            openAppSettings(requireContext())
        }
    }

    private fun showLocationRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Location Permission Needed")
            .setMessage("Location permission is required")
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dialog_view))
            .setPositiveButton("Grant") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getLastLocation() {
        // Check if permissions are granted again for safety
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it again or handle the situation
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    lifecycleScope.launch {
                        locationViewModel.getLocationData(latitude.toString(), longitude.toString())
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to get location. Is location enabled?", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error getting location: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun observeLocationState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationViewModel.locationResponseState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleLocationSuccess(state.data)
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeWeatherState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                weatherViewModel.weatherResponseState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleWeatherSuccess(state.data)
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleLocationSuccess(data: LocationResponse) {
        binding.loading.visibility = View.GONE
        locationViewModel.resetState()

        cityName = data.city
        binding.searchLocation.setQuery(cityName, false)
        lifecycleScope.launch {
            weatherViewModel.getWeatherData(cityName)
        }
    }

    private fun handleWeatherSuccess(data: WeatherResponse) {
        binding.loading.visibility = View.GONE
        weatherViewModel.resetState()
        aqiViewModel.fetchAQI(data)
        setWeatherDate(data)
    }

    private fun showError(message: String) {
        binding.loading.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e("AdminHomeDebug", "Error: $message")
        weatherViewModel.resetState()
        locationViewModel.resetState()
    }

    private fun setWeatherDate(data: WeatherResponse) {
        binding.tempValue.text = buildString {
            append(data.main.temp)
            append(" °C")
        }
        binding.humidityValue.text = buildString {
            append(data.main.humidity)
            append(" %")
        }
        binding.windValue.text = buildString {
            append(data.wind.speed)
            append(" m/s")
        }
    }

    private fun observeAQIResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                aqiViewModel.aqiResult.collect { result ->
                    if (result != null) {
                        setAQIData(result)
                        saveAqiData(result)
                    }
                }
            }
        }
    }

    private fun setAQIData(data: AQIResult) {
        binding.aqiValue.text = data.aqi.toString()
        binding.aqiCard.setCardBackgroundColor(data.color)
        binding.aqiCategory.text = data.label
        binding.impact.text = data.healthImpact
        val impactImage = getImpactImage(data.aqi)
        binding.impactImg.setImageResource(impactImage)
        updatedAt = System.currentTimeMillis()
        updateUpdatedAtText()
    }

    private fun getImpactImage(aqi: Int): Int {
        return when (aqi) {
            in 0..50 -> R.drawable.good
            in 51..100 -> R.drawable.moderate
            in 101..150 -> R.drawable.unhealthy_for_sensitive_groups
            in 151..200 -> R.drawable.unhealthy
            in 201..300 -> R.drawable.very_unhealthy
            else -> R.drawable.hazardous
        }
    }

    private fun updateUpdatedAtText() {
        val timeAgo = getTimeAgoFromTimestamp(updatedAt)
        binding.updatedAgo.text = timeAgo

        // Schedule next update in 60 seconds
        viewLifecycleOwner.lifecycleScope.launch {
            delay(60 * 1000)
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                updateUpdatedAtText()
            }
        }
    }

    private fun startRefreshLoop() {
        getLastLocation()

        // Schedule next refresh in 1 hour
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3600 * 1000)
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                startRefreshLoop()
            }
        }
    }

    private fun saveAqiData(aqiResult: AQIResult) {
        val prefsManager = UserPreferencesManager(requireContext())
        val userId = prefsManager.userId
        val date = LocalDate.now().toString()
        val updatedAt = timestampToString(updatedAt)

        val aqi = AQI(
            userId = userId,
            date = date,
            updatedAt = updatedAt,
            aqi = aqiResult.aqi,
            label = aqiResult.label,
            color = aqiResult.color,
            healthImpact = aqiResult.healthImpact,
            recommendedAction = aqiResult.recommendedAction
        )
        localAqiViewModel.saveAQI(aqi) {
            Log.d("AdminHomeDebug", "AQI data locally saved successfully")
        }

        if (isInternetAvailable(requireContext())) {
            remoteAqiViewModel.saveAqiData(aqi, date)
        }
    }
}