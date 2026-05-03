package com.aqi.weather

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aqi.weather.admin.viewModels.AdminViewModel
import com.aqi.weather.auth.viewModels.AuthViewModel
import com.aqi.weather.citizen.viewModels.CitizenViewModel
import com.aqi.weather.data.model.User
import com.aqi.weather.databinding.EditProfileSheetBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.isInternetAvailable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EditProfileBottomSheet : BottomSheetDialogFragment() {
    private val binding by lazy {
        EditProfileSheetBinding.inflate(layoutInflater)
    }
    private val citizenViewModel: CitizenViewModel by activityViewModels()
    private val adminViewModel: AdminViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private var selectedGender: String? = null
    private var currentUser: User? = null
    private var userType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve arguments
        arguments?.let { args ->
            userType = args.getString(ARG_USER_TYPE, "Citizen") // Default, if not provided
        }

        Log.d("EditProfileBottomSheet", "userType: $userType")
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

        if (userType == "Admin") {
            adminViewModel.retrieveAdminData()
            observeAdminData()
        } else {
            citizenViewModel.retrieveCitizenData()
            observeCitizenData()
        }

        setupListeners()
        observeUpdateState()
    }

    private fun observeAdminData() {
        viewLifecycleOwner.lifecycleScope.launch {
            adminViewModel.adminDataState.collectLatest { state ->
                when (state) {
                    is NetworkState.Success -> {
                        currentUser = state.data
                        populateFields(state.data)
                    }
                    is NetworkState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeCitizenData() {
        viewLifecycleOwner.lifecycleScope.launch {
            citizenViewModel.citizenDataState.collectLatest { state ->
                when (state) {
                    is NetworkState.Success -> {
                        currentUser = state.data
                        populateFields(state.data)
                    }
                    is NetworkState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun populateFields(user: User) {
        binding.name.setText(user.name)
        binding.email.setText(user.email)
        binding.dob.setText(user.dob)
        binding.phone.setText(user.phone)
        selectedGender = user.gender
        updateGenderUI()
    }

    private fun setupListeners() {
        binding.dob.setOnClickListener {
            showDatePicker(currentUser?.dob) { selectedDate ->
                binding.dob.setText(selectedDate)
            }
        }

        binding.male.setOnClickListener {
            selectedGender = "male"
            updateGenderUI()
        }

        binding.female.setOnClickListener {
            selectedGender = "female"
            updateGenderUI()
        }

        binding.saveBtn.setOnClickListener {
            saveProfile()
        }
    }

    private fun updateGenderUI() {
        when (selectedGender) {
            "male" -> {
                binding.male.setImageResource(R.drawable.male_selected)
                binding.female.setImageResource(R.drawable.female_unselected)
            }
            "female" -> {
                binding.female.setImageResource(R.drawable.female_selected)
                binding.male.setImageResource(R.drawable.male_unselected)
            }
        }
    }

    private fun showDatePicker(
        initialDate: String? = null,
        onDateSelected: (String) -> Unit
    ) {
        // Convert string to LocalDate for picker selection
        val localDate = if (!initialDate.isNullOrEmpty()) {
            LocalDate.parse(initialDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            LocalDate.now()
        }

        // CalendarConstraints to restrict future dates
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setTheme(R.style.MaterialDatePickerTheme)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val formattedDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            onDateSelected(formattedDate)
        }

        datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun saveProfile() {
        val name = binding.name.text.toString().trim()
        val phone = binding.phone.text.toString().trim()
        val dob = binding.dob.text.toString().trim()

        when {
            name.isBlank() -> {
                Toast.makeText(requireContext(), "Please enter name", Toast.LENGTH_SHORT).show()
                return
            }
            dob.isBlank() -> {
                Toast.makeText(requireContext(), "Please enter dob", Toast.LENGTH_SHORT).show()
                return
            }
            phone.isBlank() || phone.length != 11 -> {
                Toast.makeText(requireContext(), "Please enter valid phone number", Toast.LENGTH_SHORT).show()
                return
            }
            selectedGender.isNullOrEmpty() -> {
                Toast.makeText(requireContext(), "Please select gender", Toast.LENGTH_SHORT).show()
                return
            }
            !isInternetAvailable(requireContext()) -> {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                val updates = hashMapOf(
                    "name" to name,
                    "phone" to phone,
                    "dob" to dob,
                    "gender" to selectedGender
                )

                authViewModel.updateUser(updates, userType)
            }
        }
    }

    private fun observeUpdateState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.updateUserState.collectLatest { state ->
                when (state) {
                    is NetworkState.Loading -> {
                        binding.saveBtn.isEnabled = false
                        binding.saveBtn.text = "Updating..."
                    }
                    is NetworkState.Success -> {
                        binding.saveBtn.isEnabled = true
                        binding.saveBtn.text = "Save"
                        Toast.makeText(requireContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                        authViewModel.resetStates()
                        dismiss()
                    }
                    is NetworkState.Error -> {
                        binding.saveBtn.isEnabled = true
                        binding.saveBtn.text = "Save"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        authViewModel.resetStates()
                    }
                    else -> {}
                }
            }
        }
    }

    companion object {
        private const val ARG_USER_TYPE = "userType"
        fun newInstance(userType: String): EditProfileBottomSheet {
            val fragment = EditProfileBottomSheet()

            // Bundle to pass arguments
            val args = Bundle()
            args.putString(ARG_USER_TYPE, userType)
            fragment.arguments = args

            return fragment
        }
    }
}