package com.aqi.weather.auth

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aqi.weather.R
import com.aqi.weather.admin.AdminMainActivity
import com.aqi.weather.auth.viewModels.AuthViewModel
import com.aqi.weather.citizen.CitizenMainActivity
import com.aqi.weather.databinding.ActivityOtpactivityBinding
import com.aqi.weather.util.NetworkState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class OTPActivity : AppCompatActivity() {
    private val binding  by lazy {
        ActivityOtpactivityBinding.inflate(layoutInflater)
    }
    private var verificationId: String = ""
    private var auth: FirebaseAuth? = null
    private var dialog: ProgressDialog? = null
    private var resendingToken: ForceResendingToken? = null
    var timeoutSeconds: Long = 60L
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var userPreferences: SharedPreferences
    var userType: String? = null
    var otp = ""

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

        userPreferences = getSharedPreferences("LOGIN", MODE_PRIVATE)

        auth = FirebaseAuth.getInstance()

        val phoneNumber = intent.getStringExtra("phoneNum")
        userType = intent.getStringExtra("userType")

        binding.phoneLabel.text = buildString {
            append("Verify ")
            append(phoneNumber)
        }

        dialog = ProgressDialog(this@OTPActivity)
        dialog!!.setMessage("Sending OTP...")
        dialog!!.setCancelable(false)
        dialog!!.show()

        sendOtp(userType, phoneNumber, false)
        observeFirebaseAuthState()

        binding.verifyBtn.setOnClickListener {
            // 1. Check if we actually have a verificationId from Firebase
            if (verificationId.isEmpty()) {
                Toast.makeText(this, "Please wait for the OTP to be sent.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Check if the user has entered the OTP
            if (otp.length < 6) { // Adjust length based on your OTP requirement
                Toast.makeText(this, "Please enter a valid OTP.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Create credential safely
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            authViewModel.firebaseAuth(credential, userType ?: "Citizen")
        }

        binding.resendOtpTextview.setOnClickListener {
            sendOtp(userType, phoneNumber, true)
        }

        binding.otpView.setOtpCompletionListener { otp ->
            this.otp = otp
        }
    }

    private fun sendOtp(userType: String?, phoneNumber: String?, isResend: Boolean) {
        if (userType.isNullOrEmpty() || phoneNumber.isNullOrEmpty()) {
            Log.e("OTPActivityDebug", "User type or Phone number is null or empty")
            return
        }

        dialog?.show() ?: run {
            Log.e("OTPActivityDebug", "Dialog is null")
            return
        }

        startResendTimer()
        dialog!!.show()

        val builder =
            PhoneAuthOptions.newBuilder(auth!!)
                .setPhoneNumber(phoneNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                        Toast.makeText(this@OTPActivity, "Verified", Toast.LENGTH_SHORT).show()
                        authViewModel.firebaseAuth(phoneAuthCredential, userType)
                        dialog!!.dismiss()
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.w("OtpActivityDebug", "onVerificationFailed", e)
                        when (e) {
                            is FirebaseAuthInvalidCredentialsException -> {
                                Toast.makeText(this@OTPActivity, "Invalid request", Toast.LENGTH_SHORT).show()
                            }

                            is FirebaseTooManyRequestsException -> {
                                Toast.makeText(this@OTPActivity, "The SMS quota for the project has been exceeded", Toast.LENGTH_SHORT).show()
                            }

                            is FirebaseAuthMissingActivityForRecaptchaException -> {
                                Toast.makeText(this@OTPActivity, "reCAPTCHA verification attempted with null Activity", Toast.LENGTH_SHORT).show()
                            }
                        }
                        dialog!!.dismiss()
                    }

                    override fun onCodeSent(verifyId: String, forceResendingToken: ForceResendingToken) {
                        super.onCodeSent(verifyId, forceResendingToken)
                        verificationId = verifyId
                        resendingToken = forceResendingToken
                        // To show the keyboard
                        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                        windowInsetsController.show(WindowInsetsCompat.Type.ime())
                        Toast.makeText(this@OTPActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                        dialog!!.dismiss()
                        binding.otpView.requestFocus()
                    }
                })
        if (isResend) {
            resendingToken?.let {
                PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(it).build())
            } ?: run {
                Log.e("OTPActivityDebug", "Resending token is null")
            }
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    private fun startResendTimer() {
        binding.resendOtpTextview.isEnabled = false
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                timeoutSeconds--
                runOnUiThread {
                    binding.resendOtpTextview.text = "Resend OTP in $timeoutSeconds seconds"
                }
                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L
                    timer.cancel()
                    runOnUiThread {
                        binding.resendOtpTextview.text = "Send OTP again"
                        binding.resendOtpTextview. isEnabled = true
                    }
                }
            }
        }, 0, 1000)
    }

    private fun observeFirebaseAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.firebaseAuthState.collect { state ->
                    when (state) {
                        is NetworkState.Loading -> binding.loading.visibility = View.VISIBLE
                        is NetworkState.Success -> handleAuthSuccess(state.data)
                        is NetworkState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleAuthSuccess(actualUserType: String?) {
        binding.loading.visibility = View.GONE
        authViewModel.resetStates()
        if (actualUserType == userType) {
            Toast.makeText(this, "SignIn successful", Toast.LENGTH_SHORT).show()
            // Store user type in SharedPreferences
            userPreferences.edit { putString("USERTYPE", userType) }

            // Navigate to the correct dashboard
            when (userType) {
                "Admin" -> startActivity(Intent(this, AdminMainActivity::class.java))
                "Citizen" -> startActivity(Intent(this, CitizenMainActivity::class.java))
            }
            finish()
        } else {
            // Role mismatch, sign out and show error
            Firebase.auth.signOut()
            Toast.makeText(this, "Please SignIn in as the correct role.", Toast.LENGTH_SHORT).show()
            Log.d("OTPActivityDebug", "Actual User: $actualUserType, SignIn as: $userType")
        }
    }

    private fun showError(message: String) {
        binding.loading.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("OTPActivityDebug", "Error: $message")
        authViewModel.resetStates()
    }
}
