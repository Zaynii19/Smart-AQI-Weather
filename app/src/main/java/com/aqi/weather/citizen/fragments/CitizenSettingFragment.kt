package com.aqi.weather.citizen.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.EditProfileBottomSheet
import com.aqi.weather.R
import com.aqi.weather.auth.SignInOptionActivity
import com.aqi.weather.citizen.viewModels.CitizenViewModel
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.FragmentCitizenSettingBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.WorkSchedulers
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class CitizenSettingFragment : Fragment() {
    private val binding by lazy {
        FragmentCitizenSettingBinding.inflate(layoutInflater)
    }
    private val viewModel: CitizenViewModel by activityViewModels()
    private lateinit var prefsManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = UserPreferencesManager(requireContext())
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

        viewModel.retrieveCitizenData()
        observeAdminState()

        val notificationEnabled = prefsManager.notificationEnabled
        binding.notificationSwitch.isChecked = notificationEnabled

        setListeners()
    }

    private fun observeAdminState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.citizenDataState.collect { state ->
                    if (state is NetworkState.Success) {
                        viewModel.resetStates()

                        val admin = state.data
                        binding.name.text = admin.name
                        binding.email.text = admin.email
                        if (admin.gender == "male") {
                            binding.profileImage.setImageResource(R.drawable.male)
                        } else {
                            binding.profileImage.setImageResource(R.drawable.female)
                        }
                    }
                }
            }
        }
    }

    private fun setListeners() {
        binding.signOutBtn.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                Firebase.auth.signOut()
                prefsManager.clearAllPreferences()
                WorkSchedulers.cancelAqiDataSync(requireContext())
                WorkSchedulers.cancelNotificationsSchedule(requireContext())
                startActivity(Intent(requireActivity(), SignInOptionActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.profile.setOnClickListener {
            EditProfileBottomSheet.newInstance("Citizen").show(childFragmentManager, "EditProfileBottomSheet")
        }

        binding.notificationSwitch.setOnClickListener {
            if (binding.notificationSwitch.isChecked) {
                WorkSchedulers.scheduleNotifications(requireContext())
                prefsManager.savePreference(notificationEnabled = true)
                Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                WorkSchedulers.cancelNotificationsSchedule(requireContext())
                prefsManager.savePreference(notificationEnabled = false)
                Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}