package com.example.networkofone.mvvm.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class AuthenticationActivityViewModel : ViewModel() {
    private val currentTab = MutableLiveData(0)
    private val authButtonText = MutableLiveData("Signup")
    private val authMessage = MutableLiveData("Don't have an account?")
    private val message1 = MutableLiveData("Go ahead and provide your credentials")
    private val message2 = MutableLiveData("Sign-In and never lose your progress")

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
            message1.value = "Go ahead and provide your credentials"
            message2.value = "Sign-In and never lose your progress"
        } else {
            authButtonText.value = "Login"
            authMessage.value = "Already have an account?"
            message1.value = "Go ahead and set up your account"
            message2.value = "Signup and enjoy the best learning experience"
        }
    }
}