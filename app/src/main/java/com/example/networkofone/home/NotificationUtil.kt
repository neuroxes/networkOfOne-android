package com.example.networkofone.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.networkofone.R
import com.example.networkofone.activities.NotificationActivity
import com.example.networkofone.activities.SplashActivity
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.utils.SharedPrefManager

object NotificationUtil {

    fun showSystemNotification(
        context: Context,
        channelId: String = "default_channel",
        channelName: String = "General Notifications",
        notificationId: Int,
        title: String,
        message: String,
        smallIconResId: Int = R.drawable.logo_transparent,
        largeIconResId: Int? = null,
        autoCancel: Boolean = true,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "App Notifications"
        }
        notificationManager.createNotificationChannel(channel)

        // Create intent for MainActivity
        // Create the appropriate intent based on login status
        val intent = when (isUserLoggedIn(context)) {
            UserType.SCHOOL -> {
                Intent(context, NotificationActivity::class.java).apply {
                    putExtra("userType", UserType.SCHOOL)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }

            UserType.REFEREE -> {
                Intent(context, NotificationActivity::class.java).apply {
                    putExtra("userType", UserType.REFEREE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }

            else -> Intent(context, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }


        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId).setContentTitle(title)
            .setContentText(message).setSmallIcon(smallIconResId).setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel) // Dismiss when tapped
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Set large icon if provided
        largeIconResId?.let {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, it))
        }

        // Show notification
        notificationManager.notify(notificationId, builder.build())
    }

    private fun isUserLoggedIn(context: Context): UserType? {
        return SharedPrefManager(context).getUser()?.userType
    }
}