package com.aqi.weather.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aqi.weather.util.NetworkState
import com.aqi.weather.util.Security
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AuthViewModel : ViewModel() {
    private val _signUpState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val signUpState: StateFlow<NetworkState<Boolean>> = _signUpState.asStateFlow()
    private val _signInState = MutableStateFlow<NetworkState<String>>(NetworkState.Idle)
    val signInState: StateFlow<NetworkState<String>> = _signInState.asStateFlow()
    private val _updateUserState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val updateUserState: StateFlow<NetworkState<Boolean>> = _updateUserState.asStateFlow()

    private val _deleteUserState = MutableStateFlow<NetworkState<Boolean>>(NetworkState.Idle)
    val deleteUserState: StateFlow<NetworkState<Boolean>> = _deleteUserState.asStateFlow()

    fun signupUser(name: String, email: String, pass: String, selectedUserType: String) {
        _signUpState.value = NetworkState.Loading

        Firebase.auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = Firebase.auth.currentUser?.uid
                    if (userId != null) {
                        saveUser(selectedUserType, userId, name, email, pass)
                    } else {
                        _signUpState.value = NetworkState.Error("Failed to retrieve user ID after signup.")
                        Firebase.auth.currentUser?.delete()
                    }
                } else {
                    Log.e("AuthViewModelDebug", "SignUp Failed: ${task.exception?.localizedMessage}")
                    _signUpState.value = NetworkState.Error("SignUp Failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    fun loginUser(email: String, pass: String) {
        _signInState.value = NetworkState.Loading

        Firebase.auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, now get the user role
                    val userId = Firebase.auth.currentUser?.uid
                    if (userId != null) {
                        // Check both Admin and Citizen nodes
                        val database = FirebaseDatabase.getInstance()
                        val adminRef = database.getReference("Admin/$userId")
                        val citizenRef = database.getReference("Citizen/$userId")
                        // Check Admin node
                        adminRef.child("userType").get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val actualUserType = snapshot.getValue(String::class.java)
                                _signInState.value = NetworkState.Success(actualUserType ?: "Unknown")
                            } else {
                                // Check Citizen node if Admin not found
                                citizenRef.child("userType").get().addOnSuccessListener { citizenSnapshot ->
                                    if (citizenSnapshot.exists()) {
                                        val actualUserType = citizenSnapshot.getValue(String::class.java)
                                        _signInState.value = NetworkState.Success(actualUserType ?: "Unknown")
                                    } else {
                                        _signInState.value = NetworkState.Error("User type not found.")
                                    }
                                }.addOnFailureListener {
                                    Log.e("AuthViewModelDebug", "Failed to retrieve user role: ${it.localizedMessage}")
                                    _signInState.value = NetworkState.Error("Failed to retrieve user role: ${it.localizedMessage}")
                                }
                            }
                        }.addOnFailureListener {
                            Log.e("AuthViewModelDebug", "Failed to retrieve user role: ${it.localizedMessage}")
                            _signInState.value = NetworkState.Error("Failed to retrieve user role: ${it.localizedMessage}")
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    val message = when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> "No account exists with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        is FirebaseNetworkException -> "Network error. Please check your connection."
                        else -> exception?.localizedMessage ?: "Authentication failed."
                    }
                    Log.e("AuthViewModelDebug", "SignIn Failed: ${task.exception?.localizedMessage}")
                    _signInState.value = NetworkState.Error(message)
                }
            }
    }

    private fun saveUser(
        selectedUserType: String,
        userId: String,
        name: String,
        email: String,
        pass: String
    ) {
        val userRef = Firebase.database.reference.child(selectedUserType).child(userId)
        val id = userRef.push().key ?: UUID.randomUUID().toString()
        val encryptedPassword = Security.encrypt(pass)
        val user = hashMapOf(
            "userType" to selectedUserType,
            "id" to id,
            "name" to name,
            "email" to email,
            "pass" to encryptedPassword
        )

        // Store user data in Firebase Database
        userRef.setValue(user).addOnCompleteListener { storeTask ->
            if (storeTask.isSuccessful) {
                _signUpState.value = NetworkState.Success(true)
            } else {
                Log.e("AuthViewModelDebug", "Failed to save user data: ${storeTask.exception?.localizedMessage}")
                _signUpState.value = NetworkState.Error("Failed to save user data: ${storeTask.exception?.localizedMessage}")
            }
        }
    }

    fun updateUser(updates: Map<String, Any?>, pathString: String) {
        _updateUserState.value = NetworkState.Loading

        // Check if user is logged in
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val databaseRef = Firebase.database.reference.child(pathString)
                .child(currentUser.uid)

            databaseRef.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("AuthViewModelDebug", "User profile updated successfully")
                    _updateUserState.value = NetworkState.Success(true)
                }
                .addOnFailureListener { dbException ->
                    Log.e("AuthViewModelDebug", "Error updating user profile", dbException)
                    _updateUserState.value = NetworkState.Error("Failed to update profile: ${dbException.localizedMessage}")
                }
        } else {
            _updateUserState.value = NetworkState.Error("User not authenticated")
        }
    }

    fun deleteUser(pathString: String) {
        _deleteUserState.value = NetworkState.Loading

        val user = Firebase.auth.currentUser
        val userId = user?.uid

        if (userId != null) {
            val dbReference = FirebaseDatabase.getInstance().getReference(pathString).child(userId)

            // 1. Delete from Realtime Database first
            dbReference.removeValue().addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    // 2. Database entry gone! Now delete the Auth account
                    user.delete().addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Log.d("AuthViewModelDebug", "User and data deleted successfully.")
                            _deleteUserState.value = NetworkState.Success(true)
                        } else {
                            Log.e("AuthViewModelDebug", "Auth delete failed", authTask.exception)
                            _deleteUserState.value = NetworkState.Error("Failed to delete user: ${authTask.exception?.localizedMessage}")
                        }
                    }
                } else {
                    Log.e("AuthViewModelDebug", "Database deletion failed", dbTask.exception)
                    _deleteUserState.value = NetworkState.Error("Failed to delete user data: ${dbTask.exception?.localizedMessage}")
                }
            }
        } else {
            _deleteUserState.value = NetworkState.Error("User not authenticated")
        }
    }
}