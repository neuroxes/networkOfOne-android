package com.example.networkofone.fcm

import android.util.Log
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.repo.NotificationRepositoryFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FCMTokenManager {
    private val notificationRepo = NotificationRepositoryFirebase()
    
    fun initializeFCMToken(userType: UserType) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
            
            // Save token to database
            CoroutineScope(Dispatchers.IO).launch {
                notificationRepo.saveFCMToken(userId, token, userType)
                Log.d("FCM", "Token saved for user: $userId")
            }
        }
    }
    
    fun refreshFCMToken(userType: UserType) {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                initializeFCMToken(userType)
            }
        }
    }
}