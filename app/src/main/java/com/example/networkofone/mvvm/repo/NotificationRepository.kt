package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.UserType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationRepository() {
    private val database =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val ref = database.getReference("notifications")

    private var notificationListener: ValueEventListener? = null

    fun getNotificationsRealtime(userType: UserType, callback: (List<Notification>) -> Unit) {
        notificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<Notification>()

                for (notificationSnapshot in snapshot.children) {
                    try {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        notification?.let {
                            // Filter by current user's userId
                            when (userType) {
                                UserType.SCHOOL -> {
                                    if (it.userId == userId) {
                                        notifications.add(it)
                                    }
                                }

                                UserType.REFEREE -> {
                                    if (it.refereeId == userId) {
                                        notifications.add(it)
                                    }
                                }

                                else -> {}
                            }

                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification: ${e.message}")
                    }
                }

                // Sort by creation date (newest first) and return
                callback(notifications.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                callback(emptyList())
            }
        }

        ref.addValueEventListener(notificationListener!!)
    }

    // Remove the real-time listener when needed
    fun removeNotificationListener() {
        notificationListener?.let {
            ref.removeEventListener(it)
        }
    }

    // 2. One-time fetch (for backward compatibility) - filters by userId
    suspend fun getNotifications(function: (List<Notification>) -> Unit) {
        try {
            val snapshot = ref.get().await()
            val notifications = mutableListOf<Notification>()

            for (notificationSnapshot in snapshot.children) {
                try {
                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    notification?.let {
                        // Filter by current user's userId
                        if (it.userId == userId) {
                            notifications.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification: ${e.message}")
                }
            }

            function(notifications.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications: ${e.message}")
            function(emptyList())
        }
    }

    // 3. Create new notification - stores directly under notifications/notificationId
    suspend fun createNotification(notification: Notification): Boolean {
        return try {
            val notificationId = ref.push().key ?: UUID.randomUUID().toString()
            val notificationWithId = notification.copy(notificationId = notificationId)

            ref.child(notificationId).setValue(notificationWithId).await()
            Log.e(TAG, "Notification created successfully with ID: $notificationId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification: ${e.message}")
            false
        }
    }

    // 4. Get unread notification count - filters by userId and isRead = false
    suspend fun getUnreadNotificationCount(userType: UserType, callback: (Int) -> Unit) {
        try {
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unreadCount = 0

                    for (notificationSnapshot in snapshot.children) {
                        try {
                            val notification =
                                notificationSnapshot.getValue(Notification::class.java)
                            notification?.let {
                                when (userType) {
                                    UserType.SCHOOL -> {
                                        if (it.userId == userId && !it.read) {
                                            unreadCount++
                                        }
                                    }

                                    UserType.REFEREE -> {
                                        if (it.refereeId == userId && !it.read) {
                                            unreadCount++
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification for count: ${e.message}")
                        }
                    }

                    callback(unreadCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error getting unread count: ${error.message}")
                    callback(0)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread notification count: ${e.message}")
            callback(0)
        }
    }

    // Real-time unread count listener - filters by userId and isRead = false
    fun getUnreadNotificationCountRealtime(
        userType: UserType,
        callback: (List<Notification>) -> Unit,
    ): ValueEventListener {
        // Clear previous count for each new data change
        var listener: ValueEventListener? = null

        listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unreadCount = mutableListOf<Notification>() // Reset count for each update

                for (notificationSnapshot in snapshot.children) {
                    try {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        notification?.let {
                            when (userType) {
                                UserType.SCHOOL -> {
                                    if (it.userId == userId && !it.read) {
                                        unreadCount.add(it)
                                        Log.d(
                                            TAG,
                                            "onDataChange: Unread Notification -> ${it.notificationId} - ${it.read}",
                                        )
                                    }
                                }

                                UserType.REFEREE -> {
                                    if ((it.refereeId == userId && !it.read)) {
                                        unreadCount.add(it)
                                    }
                                }

                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification for count: ${e.message}")
                    }
                }
                callback(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting unread count: ${error.message}")
                callback(emptyList())
            }
        })

        return listener
    }

    // 5. Mark all notifications as read for current user
    suspend fun markAllNotificationsAsRead(userType: UserType): Boolean {
        return try {
            val snapshot = ref.get().await()
            val updates = mutableMapOf<String, Any>()

            for (notificationSnapshot in snapshot.children) {
                try {
                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    notification?.let {
                        when (userType) {
                            UserType.SCHOOL -> {
                                if (it.userId == userId && !it.read) {
                                    val notificationId = notificationSnapshot.key
                                    if (notificationId != null) {
                                        val updatedNotification = it.copy(read = true)
                                        updates[notificationId] = updatedNotification
                                    }
                                }
                            }

                            UserType.REFEREE -> {
                                if (it.refereeId == userId && !it.read) {
                                    val notificationId = notificationSnapshot.key
                                    if (notificationId != null) {
                                        val updatedNotification = it.copy(read = true)
                                        updates[notificationId] = updatedNotification
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification for update: ${e.message}")
                }
            }

            if (updates.isNotEmpty()) {
                ref.updateChildren(updates).await()
                Log.e(TAG, "All notifications marked as read for user: $userId")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notifications as read: ${e.message}")
            false
        }
    }

    // Mark specific notification as read
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            // First get the notification to ensure it belongs to current user
            val snapshot = ref.child(notificationId).get().await()
            val notification = snapshot.getValue(Notification::class.java)

            if (notification != null) {
                val updatedNotification = notification.copy(read = true)
                ref.child(notificationId).setValue(updatedNotification).await()
                Log.e(TAG, "Notification $notificationId marked as read")
                true
            } else {
                Log.w(TAG, "Notification not found or doesn't belong to current user")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}")
            false
        }
    }

    // Delete notification (only if it belongs to current user)
    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            // First get the notification to ensure it belongs to current user
            val snapshot = ref.child(notificationId).get().await()
            val notification = snapshot.getValue(Notification::class.java)

            if (notification != null) {
                ref.child(notificationId).removeValue().await()
                Log.e(TAG, "Notification $notificationId deleted")
                true
            } else {
                Log.w(TAG, "Notification not found or doesn't belong to current user")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification: ${e.message}")
            false
        }
    }

    // Update notification (only if it belongs to current user)
    suspend fun updateNotification(notification: Notification): Boolean {
        return try {
            // Ensure the notification belongs to current user
            ref.child(notification.notificationId).setValue(notification).await()
            Log.e(TAG, "Notification ${notification.notificationId} updated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "Notification_Repo"
    }
}