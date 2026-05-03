package com.aqi.weather.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.EditProfileBottomSheet
import com.aqi.weather.R
import com.aqi.weather.admin.AqiThresholdBottomSheet
import com.aqi.weather.admin.CitizensActivity
import com.aqi.weather.admin.viewModels.AdminViewModel
import com.aqi.weather.auth.SignInOptionActivity
import com.aqi.weather.data.local.preference.UserPreferencesManager
import com.aqi.weather.databinding.FragmentAdminSettingBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.WorkSchedulers
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class AdminSettingFragment : Fragment() {
    private val binding by lazy {
        FragmentAdminSettingBinding.inflate(layoutInflater)
    }
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.retrieveAdminData()
        observeAdminState()
        setListeners()
    }

    private fun observeAdminState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.adminDataState.collect { state ->
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
                val prefsManager = UserPreferencesManager(requireContext())
                prefsManager.clearAllPreferences()
                WorkSchedulers.cancelAqiDataSync(requireContext())
                startActivity(Intent(requireActivity(), SignInOptionActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.profile.setOnClickListener {
            EditProfileBottomSheet.newInstance("Admin").show(childFragmentManager, "EditProfileBottomSheet")
        }

        binding.citizens.setOnClickListener {
            startActivity(Intent(requireActivity(), CitizensActivity::class.java))
        }

        binding.threshold.setOnClickListener {
            AqiThresholdBottomSheet.newInstance().show(childFragmentManager, "AqiThresholdBottomSheet")
        }
    }
}