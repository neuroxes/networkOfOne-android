package com.example.networkofone.fcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.networkofone.activities.SplashActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle notification based on type
        val notificationType = remoteMessage.data["type"]
        val gameId = remoteMessage.data["gameId"]
        val paymentRequestId = remoteMessage.data["paymentRequestId"]

        // Create and show notification
        showNotification(
            remoteMessage.notification?.title ?: "",
            remoteMessage.notification?.body ?: "",
            notificationType,
            gameId,
            paymentRequestId
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String?,
        gameId: String?,
        paymentId: String?,
    ) {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("type", type)
        intent.putExtra("gameId", gameId)
        intent.putExtra("paymentId", paymentId)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel" // You should define this channel ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's notification icon
            .setContentTitle(title).setContentText(body).setAutoCancel(true)
            .setSound(defaultSoundUri).setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}