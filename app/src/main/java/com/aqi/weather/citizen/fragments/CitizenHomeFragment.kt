package com.aqi.weather.citizen.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aqi.weather.databinding.FragmentCitizenHomeBinding
import com.aqi.weather.sharedViewModels.LocalAqiViewModel
import com.aqi.weather.sharedViewModels.LocationViewModel
import com.aqi.weather.sharedViewModels.RemoteAqiViewModel
import com.aqi.weather.sharedViewModels.WeatherViewModel
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.getTimeAgoFromTimestamp
import com.aqi.weather.util.hasLocationPermission
import com.aqi.weather.util.hasNotificationPermission
import com.aqi.weather.util.isInternetAvailable
import com.aqi.weather.util.openAppSettings
import com.aqi.weather.util.timestampToString
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.getValue

class CitizenHomeFragment : Fragment() {
    private val binding by lazy {
        FragmentCitizenHomeBinding.inflate(layoutInflater)
    }
    private val locationViewModel: LocationViewModel by viewModels()
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val aqiViewModel: AQIViewModel by viewModels()
    private val localAqiViewModel: LocalAqiViewModel by viewModels()
    private val remoteAqiViewModel: RemoteAqiViewModel by viewModels()
    private var cityName = "Islamabad"
    private var updatedAt = System.currentTimeMillis()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Single launcher for ALL permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Check Location Permission
        val isLocationGranted = hasLocationPermission(requireContext())

        // Check Notification Permission (Android 13+)
        val isNotificationGranted = hasNotificationPermission(requireContext())

        // Handle Location Permission
        if (isLocationGranted) {
            getLastLocation()
        } else {
            handlePermissionDenied(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                permissionName = "Location"
            )
        }

        // Handle Notification Permission
        if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            handlePermissionDenied(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                permissionName = "Notification"
            )
        } else if (isNotificationGranted) {
            Log.d("CitizenHomeDebug", "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isInternetAvailable(requireContext())) {
            checkAllPermissions()
        } else {
            Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
        }

        observeLocationState()
        observeWeatherState()
        observeAQIResult()
        setupSwipeRefresh()
        setSearchView()
        startRefreshLoop()

        binding.locationBtn.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                checkAllPermissions()
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Single method to check and request ALL permissions
    private fun checkAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check Location Permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request all missing permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            getLastLocation()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.blue)
        binding.swipeRefresh.setOnRefreshListener {
            if (isInternetAvailable(requireContext())) {
                lifecycleScope.launch {
                    weatherViewModel.getWeatherData(cityName)
                }
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setSearchView() {
        binding.searchLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (isInternetAvailable(requireContext())) {
                    query?.let { cityName ->
                        if (cityName.isNotBlank()) {
                            this@CitizenHomeFragment.cityName = cityName
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

    private fun handlePermissionDenied(permission: String, permissionName: String) {
        Log.d("CitizenHomeDebug", "$permissionName permission denied")
        if (shouldShowRequestPermissionRationale(permission)) {
            showPermissionRationaleDialog(permissionName)
        } else {
            Toast.makeText(requireContext(), "$permissionName permission is required for full functionality", Toast.LENGTH_LONG).show()
            // Only open settings if it's a critical permission
            if (permissionName == "Location") {
                openAppSettings(requireContext())
            }
        }
    }

    private fun showPermissionRationaleDialog(permissionName: String) {
        val (title, message) = when (permissionName) {
            "Location" -> Pair(
                "Location Permission Needed",
                "Location permission is required to get your current location for accurate weather and AQI data."
            )
            "Notification" -> Pair(
                "Notification Permission Needed",
                "Notification permission is required to show you weather and AQI alerts."
            )
            else -> Pair(
                "Permission Needed",
                "$permissionName permission is required for this feature."
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dialog_view))
            .setPositiveButton("Grant") { _, _ ->
                checkAllPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        Log.e("CitizenHomeDebug", "Error: $message")
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

        viewLifecycleOwner.lifecycleScope.launch {
            delay(60 * 1000)
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                updateUpdatedAtText()
            }
        }
    }

    private fun startRefreshLoop() {
        getLastLocation()

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
            Log.d("CitizenHomeDebug", "AQI data locally saved successfully")
        }

        if (isInternetAvailable(requireContext())) {
            remoteAqiViewModel.saveAqiData(aqi, date)
        }
    }
}