package com.aqi.weather.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.AqiThresholdBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AqiThresholdBottomSheet : BottomSheetDialogFragment() {
    private val binding by lazy {
        AqiThresholdBottomSheetBinding.inflate(layoutInflater)
    }
    private lateinit var prefsManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = UserPreferencesManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPicker()

        binding.saveBtn.setOnClickListener {
            val newValue = binding.aqiPicker.value
            prefsManager.savePreference(aqiThreshold = newValue)
            Toast.makeText(requireContext(), "Threshold updated to $newValue", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun setupPicker() {
        binding.aqiPicker.minValue = 0
        binding.aqiPicker.maxValue = 500

        val currentThreshold = prefsManager.aqiThreshold
        // If 0, show a sensible default like 100
        binding.aqiPicker.value = if (currentThreshold > 0) currentThreshold else 100
    }

    companion object {
        fun newInstance() = AqiThresholdBottomSheet()
    }
}