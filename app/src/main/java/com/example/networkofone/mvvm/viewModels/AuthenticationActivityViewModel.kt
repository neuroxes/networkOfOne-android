package com.example.networkofone.mvvm.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class AuthenticationActivityViewModel : ViewModel() {
    private val currentTab = MutableLiveData(0)
    private val authButtonText = MutableLiveData("Signup")
    private val authMessage = MutableLiveData("Don't have an account?")
    private val message1 = MutableLiveData("Welcome back! Enter your credentials.")
    private val message2 = MutableLiveData("Sign in to access your account.")

    fun getCurrentTab(): LiveData<Int> {
        return currentTab
    }

    fun getAuthButtonText(): LiveData<String> {
        return authButtonText
    }

    fun getAuthMessage(): LiveData<String> {
        return authMessage
    }

    fun getMessage1(): LiveData<String> {
        return message1
    }

    fun getMessage2(): LiveData<String> {
        return message2
    }

    fun setCurrentTab(position: Int) {
        currentTab.value = position
        if (position == 0) {
            authButtonText.value = "Signup"
            authMessage.value = "Don't have an account?"
            message1.value = "Welcome back! Enter your credentials."
            message2.value = "Sign in to access your account."
        } else {
            authButtonText.value = "Login"
            authMessage.value = "Already have an account?"
            message1.value = "Create your account to start your journey."
            message2.value = "Join us and explore new possibilities."
        }
    }
}