package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.UserType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database


class SignupViewModel() : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    var userType: UserType = UserType.SCHOOL

    private val _signupState = MutableLiveData<AuthenticationResponses<String>>()
    val signupState: LiveData<AuthenticationResponses<String>> = _signupState

    fun createUser(name: String, email: String, phone: String, password: String) {

        _signupState.value = AuthenticationResponses.Loading
        creatingUser(name, email, phone, password) { result ->
            _signupState.value = result
        }
    }

    fun creatingUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        callback: (AuthenticationResponses<String>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.uid ?: return@addOnCompleteListener

                    val userModel = UserModel(
                        id = user.uid,
                        name = name,
                        email = email,
                        phone = phone,
                        userType = userType
                    )
                    Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                        .child("users")
                        .child(user.uid)
                        .setValue(userModel) // Directly set the model object
                        .addOnSuccessListener {
                            Log.e("TAG", "creatingUser: User Created Successfully.")

                            user.sendEmailVerification()
                                .addOnCompleteListener { emailTask ->
                                    if (emailTask.isSuccessful) {
                                        callback(AuthenticationResponses.Success("Please verify your email and login."))
                                    } else {
                                        callback(AuthenticationResponses.Error("Failed to send verification email."))
                                    }
                                }

                            // Update the user's displayName
                            /*
                                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                                            .setDisplayName(name)
                                                            .setPhotoUri(phone.toUri())
                                                            .build()
                            */
                            /*user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { emailTask ->
                                            if (emailTask.isSuccessful) {
                                                callback(AuthenticationResponses.Success("Please verify your email and login."))
                                            } else {
                                                callback(AuthenticationResponses.Error("Failed to send verification email."))
                                            }
                                        }
                                } else {
                                    callback(AuthenticationResponses.Error("Failed to update user profile."))
                                }
                            }*/
                        }
                        .addOnFailureListener { e ->
                            Log.e("TAG", "Error: ${e.message}")
                        }

                } else {
                    callback(AuthenticationResponses.Error("Registration Failed: ${task.exception?.message}"))
                }
            }
    }

}


sealed class AuthenticationResponses<out T> {
    data class Success<out T>(val data: T) : AuthenticationResponses<T>()
    data class Error(val message: String) : AuthenticationResponses<Nothing>()
    data object Loading : AuthenticationResponses<Nothing>()
}

