package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.FCMToken
import com.example.networkofone.mvvm.models.NotificationData
import com.example.networkofone.mvvm.models.UserType
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.messaging.messaging
import com.squareup.okhttp.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NotificationRepositoryFirebase {
    private val database = Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val tokensRef = database.getReference("fcm_tokens")
    private val messaging = Firebase.messaging

    // Store FCM token for user
    suspend fun saveFCMToken(userId: String, token: String, userType: UserType): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fcmToken = FCMToken(
                    userId = userId,
                    token = token,
                    userType = userType
                )
                tokensRef.child(userId).setValue(fcmToken).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // Get FCM token for specific user
    private suspend fun getFCMToken(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val snapshot = tokensRef.child(userId).get().await()
            snapshot.getValue(FCMToken::class.java)?.token
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error getting FCM token for user $userId", e)
            null
        }
    }

    // Send notification using FCM Admin SDK (server-side implementation)
    suspend fun sendNotification(notificationData: NotificationData): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val token = getFCMToken(notificationData.targetUserId)
                    ?: return@withContext Result.failure(Exception("FCM token not found for user"))

                // This would typically be done on your backend server
                // For client-side, you'll need to call your backend API
                sendNotificationToServer(notificationData, token)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // Call your backend API to send notification
    // In NotificationRepositoryFirebase
    private suspend fun sendNotificationToServer(notificationData: NotificationData, token: String) {
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("targetUserId", notificationData.targetUserId)
            put("notificationData", JSONObject().apply {
                put("title", notificationData.title)
                put("body", notificationData.body)
                put("type", notificationData.type.name)
                put("gameId", notificationData.gameId)
                put("paymentRequestId", notificationData.paymentRequestId)
            })
        }

        /*val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("YOUR_BACKEND_URL/api/notifications/send")
            .post(requestBody)
            .build()

        client.newCall(request).execute()*/
    }
}
