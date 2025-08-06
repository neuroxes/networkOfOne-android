package com.example.networkofone.mvvm.viewModels

import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest


class SignupViewModel() : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    var isProf: Boolean = false

    private val _signupState = MutableLiveData<AuthenticationResponses<String>>()
    val signupState: LiveData<AuthenticationResponses<String>> = _signupState

    fun createUser(name: String, email: String,phone:String, password: String) {

        _signupState.value = AuthenticationResponses.Loading
        creatingUser(name, email, phone,password) { result ->
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
                    // Update the user's displayName
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .setPhotoUri(phone.toUri())
                        .build()

                    user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
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

