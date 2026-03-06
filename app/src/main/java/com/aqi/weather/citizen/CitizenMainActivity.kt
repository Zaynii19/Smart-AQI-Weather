package com.aqi.weather.citizen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import com.aqi.weather.auth.AuthViewModel
import com.aqi.weather.citizen.citizenViewModels.CitizenViewModel
import com.aqi.weather.R
import com.aqi.weather.auth.SignInActivity
import com.aqi.weather.auth.UserModel
import com.aqi.weather.databinding.ActivityCitizenMainBinding
import com.aqi.weather.databinding.EditProfileSheetBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.isInternetAvailable
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CitizenMainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityCitizenMainBinding.inflate(layoutInflater)
    }
    private val citizenViewModel: CitizenViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var citizenObj: UserModel? = null
    private var isProfileSheetShowing = false

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

        citizenViewModel.retrieveCitizenData()
        observeCitizenDataState()
        observeUpdateState()
        observeDeleteUserState()

        binding.editBtn.setOnClickListener {
            showStartSleepDialog(citizenObj)
        }

        binding.signOutBtn.setOnClickListener {
            if (isInternetAvailable(this)) {
                Firebase.auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.deleteProfileBtn.setOnClickListener {
            if (isInternetAvailable(this)) {
                authViewModel.deleteUser("Citizen")
            } else {
                Toast.makeText(this, "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeCitizenDataState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                citizenViewModel.citizenDataState.collect { state ->
                    when {
                        !isInternetAvailable(this@CitizenMainActivity) -> showError(
                            "No internet available, please check your connection."
                        )
                        state is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        state is NetworkState.Success -> showCitizens(state.data)
                        state is NetworkState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeUpdateState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.updateUserState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> citizenViewModel.retrieveCitizenData()  // Refresh the data
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeDeleteUserState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.deleteUserState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> {
                            Firebase.auth.signOut()
                            startActivity(Intent(this@CitizenMainActivity, SignInActivity::class.java))
                            finish()
                        }
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    fun showCitizens(citizen: UserModel) {
        citizenObj = citizen
        binding.loading.visibility = View.GONE
        binding.name.text = citizen.name
        binding.email.text = citizen.email
        binding.dob.text = citizen.dob
        binding.phone.text = citizen.phone
        when (citizen.gender) {
            "male" -> {
                binding.male.setImageResource(R.drawable.male_selected)
                binding.female.setImageResource(R.drawable.female_unselected)
                binding.profileImage.setImageResource(R.drawable.male)
            }
            "female" -> {
                binding.female.setImageResource(R.drawable.female_selected)
                binding.male.setImageResource(R.drawable.male_unselected)
                binding.profileImage.setImageResource(R.drawable.female)
            }
            else -> {}
        }
    }

    private fun showError(message: String) {
        binding.loading.visibility = View.GONE
        Toast.makeText(this, "Error: $message", Toast.LENGTH_SHORT).show()
    }

    private fun showStartSleepDialog(citizen: UserModel?) {
        if (isProfileSheetShowing) {
            return
        }

        if (citizen == null) {
            Toast.makeText(this, "Citizen data not available", Toast.LENGTH_SHORT).show()
            return
        }

        isProfileSheetShowing = true
        val binder = EditProfileSheetBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this, R.style.BottomSheetMaterialDialogTheme)
            .setView(binder.root)
            .create()

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set the initial values
        binder.name.setText(citizen.name)
        binder.email.setText(citizen.email)
        binder.dob.setText(citizen.dob)
        binder.phone.setText(citizen.phone)
        when (citizen.gender) {
            "male" -> {
                binder.male.setImageResource(R.drawable.male_selected)
                binder.female.setImageResource(R.drawable.female_unselected)
            }
            "female" -> {
                binder.female.setImageResource(R.drawable.female_selected)
                binder.male.setImageResource(R.drawable.male_unselected)
            }
            else -> {}
        }

        binder.email.setOnClickListener {
            Toast.makeText(this, "Can't edit email", Toast.LENGTH_SHORT).show()
        }

        binder.dob.setOnClickListener {
            showDatePicker(citizen.dob) { selectedDate ->
                binder.dob.setText(selectedDate)
            }
        }

        binder.male.setOnClickListener {
            citizen.gender = "male"
            binder.male.setImageResource(R.drawable.male_selected)
            binder.female.setImageResource(R.drawable.female_unselected)
        }

        binder.female.setOnClickListener {
            citizen.gender = "female"
            binder.female.setImageResource(R.drawable.female_selected)
            binder.male.setImageResource(R.drawable.male_unselected)
        }

        binder.saveBtn.setOnClickListener {
            citizen.name = binder.name.text.toString()
            citizen.dob = binder.dob.text.toString()
            citizen.phone = binder.phone.text.toString()
            when {
                citizen.name.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                citizen.dob.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please enter dob", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                citizen.phone.isNullOrEmpty() || citizen.phone?.length != 11 -> {
                    Toast.makeText(this, "Please enter valid phone number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                citizen.gender.isNullOrEmpty() -> {
                    Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !isInternetAvailable(this) -> {
                    Toast.makeText(this, "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    val updates = hashMapOf<String, Any?>(
                        "name" to citizen.name,
                        "dob" to citizen.dob,
                        "gender" to citizen.gender,
                        "phone" to citizen.phone
                    )
                    authViewModel.updateUser(updates, "Citizen")
                    dialog.dismiss()
                    isProfileSheetShowing = false
                }
            }
        }

        dialog.setOnDismissListener {
            isProfileSheetShowing = false
        }

        dialog.show()

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
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

        datePicker.show(this.supportFragmentManager, "MATERIAL_DATE_PICKER")
    }
}