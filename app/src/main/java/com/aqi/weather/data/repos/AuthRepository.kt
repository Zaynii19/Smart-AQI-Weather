package com.aqi.weather.data.repos

import android.util.Log
import com.aqi.weather.util.Security
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import java.util.UUID

class AuthRepository {
    fun firebaseAuth(
        credential: AuthCredential,
        selectedUserType: String,
        onResult: (Result<String>) -> Unit
    ) {
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(Result.failure(task.exception ?: Exception("Firebase auth failed")))
                    return@addOnCompleteListener
                }

                val firebaseUser = Firebase.auth.currentUser
                val userId = firebaseUser?.uid ?: return@addOnCompleteListener
                val email = firebaseUser.email.orEmpty()
                val name = firebaseUser.displayName.orEmpty()
                val provider = firebaseUser.providerData.getOrNull(1)?.providerId
                    ?.substringBefore(".") ?: "password"

                val database = FirebaseDatabase.getInstance()
                val adminRootRef = database.getReference("Admin")
                val citizenRef = database.getReference("Citizen/$userId")

                // 1. Check if THIS specific UID is already an admin
                adminRootRef.child(userId).get().addOnSuccessListener { adminSnapshot ->
                    if (adminSnapshot.exists()) {
                        onResult(Result.success("Admin"))
                        return@addOnSuccessListener
                    }

                    // 2. If user is NOT an admin, check if they are already a Citizen
                    citizenRef.get().addOnSuccessListener { citizenSnapshot ->
                        if (citizenSnapshot.exists()) {
                            onResult(Result.success("Citizen"))
                            return@addOnSuccessListener
                        }

                        // 3. User is NEW. Now handle the "One Admin Only" restriction
                        if (selectedUserType == "Admin") {
                            // Check if ANY admin exists in the entire "Admin" node
                            adminRootRef.limitToFirst(1).get().addOnSuccessListener { allAdminsSnapshot ->
                                if (allAdminsSnapshot.hasChildren()) {
                                    // An admin already exists in the system! Block this request.
                                    onResult(Result.failure(Exception("Registration Failed: An Admin already exists.")))
                                    // Delete the Firebase Auth account to prevent ghost users
                                    firebaseUser.delete()
                                } else {
                                    // No admin exists yet. Allow this first one.
                                    saveUser(selectedUserType, userId, name, email, provider = provider)
                                    onResult(Result.success("Admin"))
                                }
                            }
                        } else {
                            // Regular Citizen registration
                            saveUser(selectedUserType, userId, name, email, provider = provider)
                            onResult(Result.success("Citizen"))
                        }
                    }.addOnFailureListener { onResult(Result.failure(it)) }
                }.addOnFailureListener { onResult(Result.failure(it)) }
            }
    }

    fun signupUser(
        name: String,
        email: String,
        pass: String,
        selectedUserType: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        Firebase.auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = Firebase.auth.currentUser?.uid
                    val provider = "email"
                    if (userId != null) {
                        onResult(Result.success(true))
                        saveUser(selectedUserType, userId, name, email, pass, provider)
                    } else {
                        onResult(Result.failure(Exception("Failed to retrieve user ID after signup.")))
                        Firebase.auth.currentUser?.delete()
                    }
                } else {
                    Log.e(
                        "AuthViewRepoDebug",
                        "SignUp Failed: ${task.exception?.localizedMessage}"
                    )
                    onResult(Result.failure(task.exception ?: Exception("SignUp Failed")))
                }
            }
    }

    private fun saveUser(
        selectedUserType: String,
        userId: String,
        name: String,
        email: String,
        pass: String? = null,
        provider: String
    ) {
        val userRef = Firebase.database.reference.child(selectedUserType).child(userId)
        val id = userRef.push().key ?: UUID.randomUUID().toString()
        val encryptedPassword = pass?.let { Security.encrypt(it) }
        val user = hashMapOf(
            "userType" to selectedUserType,
            "id" to id,
            "name" to name,
            "email" to email,
            "pass" to encryptedPassword,
            "provider" to provider
        )

        userRef.setValue(user).addOnCompleteListener { storeTask ->
            if (!storeTask.isSuccessful) {
                Log.e(
                    "AuthViewRepoDebug",
                    "Failed to save user data: ${storeTask.exception?.localizedMessage}"
                )
            }
        }
    }

    fun loginUser(email: String, pass: String, onResult: (Result<String>) -> Unit) {
        Firebase.auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = Firebase.auth.currentUser?.uid
                    if (userId != null) {
                        val database = FirebaseDatabase.getInstance()
                        val adminRef = database.getReference("Admin/$userId")
                        val citizenRef = database.getReference("Citizen/$userId")

                        adminRef.child("userType").get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val actualUserType = snapshot.getValue(String::class.java)
                                onResult(Result.success(actualUserType ?: "Unknown"))
                            } else {
                                citizenRef.child("userType").get()
                                    .addOnSuccessListener { citizenSnapshot ->
                                        if (citizenSnapshot.exists()) {
                                            val actualUserType = citizenSnapshot.getValue(String::class.java)
                                            onResult(Result.success(actualUserType ?: "Unknown"))
                                        } else {
                                            onResult(Result.failure(Exception("User type not found.")))
                                        }
                                    }.addOnFailureListener {
                                    Log.e(
                                        "AuthViewRepoDebug",
                                        "Failed to retrieve user role: ${it.localizedMessage}"
                                    )
                                    onResult(Result.failure(it))
                                }
                            }
                        }.addOnFailureListener {
                            Log.e(
                                "AuthViewRepoDebug",
                                "Failed to retrieve user role: ${it.localizedMessage}"
                            )
                            onResult(Result.failure(it))
                        }
                    } else {
                        onResult(Result.failure(Exception("Failed to retrieve user ID after login.")))
                    }
                } else {
                    val message = when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> "No account exists with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        is FirebaseNetworkException -> "Network error. Please check your connection."
                        else -> exception?.localizedMessage ?: "Authentication failed."
                    }
                    Log.e(
                        "AuthViewRepoDebug",
                        "SignIn Failed: ${task.exception?.localizedMessage}"
                    )
                    onResult(Result.failure(Exception(message)))
                }
            }
    }

    fun updateUser(
        updates: Map<String, Any?>,
        pathString: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val databaseRef = Firebase.database.reference.child(pathString).child(currentUser.uid)

            databaseRef.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("AuthViewRepoDebug", "User profile updated successfully")
                    onResult(Result.success(true))
                }
                .addOnFailureListener { dbException ->
                    Log.e("AuthViewRepoDebug", "Error updating user profile", dbException)
                    onResult(Result.failure(Exception("Failed to update profile: ${dbException.localizedMessage}")))
                }
        } else {
            onResult(Result.failure(Exception("User not authenticated")))
        }
    }

    fun deleteUser(pathString: String, onResult: (Result<Boolean>) -> Unit) {
        val user = Firebase.auth.currentUser
        val userId = user?.uid

        if (userId != null) {
            val dbReference = FirebaseDatabase.getInstance().getReference(pathString).child(userId)
            dbReference.removeValue().addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    user.delete().addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Log.d("AuthViewRepoDebug", "User and data deleted successfully.")
                            onResult(Result.success(true))
                        } else {
                            Log.e("AuthViewRepoDebug", "Auth delete failed", authTask.exception)
                            onResult(
                                Result.failure(
                                    authTask.exception ?: Exception("Failed to delete user")
                                )
                            )
                        }
                    }
                } else {
                    Log.e("AuthViewRepoDebug", "Database deletion failed", dbTask.exception)
                    onResult(
                        Result.failure(
                            dbTask.exception ?: Exception("Failed to delete user data")
                        )
                    )
                }
            }
        } else {
            onResult(Result.failure(Exception("User not authenticated")))
        }
    }
}