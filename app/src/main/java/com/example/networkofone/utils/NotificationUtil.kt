package com.example.networkofone.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.networkofone.R
import com.example.networkofone.activities.NotificationActivity
import com.example.networkofone.activities.SplashActivity
import com.example.networkofone.mvvm.models.UserType

object NotificationUtil {

    /**
     * Check if notification permission is granted (for Android 13+)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Request notification permission (for Android 13+)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode
        )
    }

    /**
     * Show system notification with permission checks
     */
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
        onPermissionDenied: (() -> Unit)? = null,
    ) {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!areNotificationsEnabled(context)) {
                onPermissionDenied?.invoke()
                return
            }
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "App Notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create the appropriate intent based on login status
        val intent = when (isUserLoggedIn(context)) {
            UserType.SCHOOL -> {
                Intent(context, NotificationActivity::class.java).apply {
                    putExtra("userType", UserType.SCHOOL)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }

            UserType.REFEREE -> {
                Intent(context, NotificationActivity::class.java).apply {
                    putExtra("userType", UserType.REFEREE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            .setAutoCancel(autoCancel).setPriority(NotificationCompat.PRIORITY_DEFAULT)

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

    /**
     * Helper function to check if we should show rationale for notification permission
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }
}