package com.example.networkofone.mvvm.viewModels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class LoginViewModel() : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginResult = MutableLiveData<AuthResult>()
    val loginResult: LiveData<AuthResult> get() = _loginResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun login(email: String, password: String) {
        _loading.value = true

        loginUser(email, password) { result ->
            _loading.value = false
            _loginResult.value = result
        }
    }

    private val _resetStatus = MutableLiveData<AuthResult>()
    val resetStatus: LiveData<AuthResult> get() = _resetStatus

    private val _loadingDialog = MutableLiveData<Boolean>()
    val loadingDialog: LiveData<Boolean> get() = _loadingDialog

    fun resetPassword(email: String) {
        _loadingDialog.value = true
        sendPasswordResetEmail(email) { result ->
            _loadingDialog.value = false
            _resetStatus.value = result
        }
    }

    suspend fun getOnBoardingStatusOfUser(): Boolean {
        return getOnBoardingStatusFromFirebase()
    }

    fun loginUser(email: String, password: String, callback: (AuthResult) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        callback(AuthResult.Success)
                    } else {
                        callback(AuthResult.EmailNotVerified)
                    }
                } else {
                    callback(AuthResult.Failure(task.exception?.message ?: "Unknown error"))
                }
            }
    }

    fun sendPasswordResetEmail(email: String, callback: (AuthResult) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(AuthResult.Success)
                } else {
                    callback(AuthResult.Failure(task.exception?.message ?: "Unknown error"))
                }
            }
    }

    suspend fun getOnBoardingStatusFromFirebase(): Boolean {
        val user = auth.currentUser ?: return false
        Log.e(TAG, "getOnBoardingStatusFromFirebase: User is not Null -> ${auth.uid}")
        val userId = user.uid
        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("UsersResponses")
            .child(userId)

        return try {
            val snapshot = databaseRef.get().await()
            Log.e(TAG, "getOnBoardingStatusFromFirebase: snapShot : ${snapshot.exists()}")
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "getOnBoardingStatusFromFirebase: snapShot : Exception Running")
            false
        }
    }



    sealed class AuthResult {
        data object Success : AuthResult()
        data object EmailNotVerified : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }

}


