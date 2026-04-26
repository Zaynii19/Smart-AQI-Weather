package com.aqi.weather.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
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
import com.aqi.weather.admin.AdminMainActivity
import com.aqi.weather.admin.viewModels.AdminViewModel
import com.aqi.weather.auth.viewModels.AuthViewModel
import com.aqi.weather.citizen.CitizenMainActivity
import com.aqi.weather.databinding.ActivitySignupBinding
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.Security
import com.aqi.weather.util.isInternetAvailable
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }
    private var selectedUserType: String = "Citizen"
    private var name: String = ""
    private var email: String = ""
    private var pass: String = ""
    private var encryptedPassword: String = ""
    private val authViewModel: AuthViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
        )
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adminViewModel.retrieveAdmins()
        observeAdminState()
        setupErrorClearingTextWatchers()
        observeSignUpState()

        binding.signupBtn.setOnClickListener {
            when {
                !isInternetAvailable(this) -> showError("No internet available, please check your connection.")
                else -> {
                    if (validateAllFields()) {
                        authViewModel.signupUser(name, email, pass, selectedUserType)
                    }
                }
            }
        }
    }

    private fun observeAdminState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                adminViewModel.adminState.collect { state ->
                    if (state is NetworkState.Success) {
                        if (state.data.isEmpty()) {
                            binding.toggleContainer.visibility = View.VISIBLE
                            onUserSelection()
                            adminViewModel.resetStates()
                        }
                    }
                }
            }
        }
    }

    private fun onUserSelection() {
        selectedUserType = binding.adminBtn.text.toString()
        binding.adminBtn.setOnClickListener {
            selectedUserType = binding.adminBtn.text.toString()
            binding.adminBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.citizenBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.adminBtn.setTextColor(Color.WHITE)
            binding.citizenBtn.setTextColor(getColor(R.color.dark_gray))
        }

        binding.citizenBtn.setOnClickListener {
            selectedUserType = binding.citizenBtn.text.toString()
            binding.citizenBtn.setBackgroundResource(R.drawable.toggle_btn_selected)
            binding.adminBtn.setBackgroundResource(R.drawable.toggle_btn_unselected)
            binding.citizenBtn.setTextColor(Color.WHITE)
            binding.adminBtn.setTextColor(getColor(R.color.dark_gray))
        }
    }

    private fun validateAllFields(): Boolean {
        var isValid = true

        // Validate name
        if (!isValidName(binding.name.text.toString())) {
            binding.nameError.visibility = View.VISIBLE
            binding.nameError.text = getString(R.string.name_validity)
            isValid = false
        } else {
            name = binding.name.text.toString()
        }

        // Validate email
        if (!isEmailValid(binding.email.text.toString())) {
            binding.emailError.visibility = View.VISIBLE
            binding.emailError.text = getString(R.string.email_validity)
            isValid = false
        } else {
            email = binding.email.text.toString()
        }

        // Validate password
        if (!isPasswordValid(binding.pass.text.toString())) {
            binding.passError.visibility = View.VISIBLE
            binding.passError.text = getString(R.string.password_validity)
            isValid = false
        } else {
            pass = binding.pass.text.toString()
            encryptedPassword = Security.encrypt(pass)
        }

        return isValid
    }

    private fun observeSignUpState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.signUpState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleSignUpSuccess()
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleSignUpSuccess() {
        binding.loading.visibility = View.GONE
        authViewModel.resetStates()
        Toast.makeText(this@SignupActivity, "SignUp Successful", Toast.LENGTH_SHORT).show()
        val intent = if (selectedUserType == "Admin")
            Intent(this@SignupActivity, AdminMainActivity::class.java)
        else
            Intent(this@SignupActivity, CitizenMainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.loading.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("SignupDebug", "Error: $message")
        authViewModel.resetStates()
    }

    private fun isValidName(name: String): Boolean {
        val namePattern = "^[a-zA-Z\\s]+$"
        return name.matches(namePattern.toRegex())
    }

    private fun isEmailValid(email: String): Boolean {
        if (email.length > 254) return false // Quick length check first
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%?&#])[A-Za-z\\d@$!%?&#]{8,}$")
        return passwordRegex.matches(password)
    }

    private fun setupErrorClearingTextWatchers() {
        // TextWatcher for email field
        binding.name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.nameError.visibility = View.GONE
            }
        })

        // TextWatcher for email field
        binding.email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.emailError.visibility = View.GONE
            }
        })

        // TextWatcher for password field
        binding.pass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.passError.visibility = View.GONE
            }
        })
    }
}