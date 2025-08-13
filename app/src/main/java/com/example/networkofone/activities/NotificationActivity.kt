package com.example.networkofone.activities

import android.app.NotificationManager;
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.networkofone.adapters.NotificationDatesAdapter
import com.example.networkofone.databinding.ActivityNotificationsBinding
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.NotificationDate
import com.example.networkofone.mvvm.models.NotificationTypeLocal
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.viewModels.NotificationActViewModel
import com.example.networkofone.utils.SharedPrefManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationDatesAdapter: NotificationDatesAdapter
    private val viewModel: NotificationActViewModel by viewModels()
    private var userType: UserType? = null
    private var notificationList: List<Notification> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userType = SharedPrefManager(this@NotificationActivity).getUser()?.userType
        // Clear all system notifications created by this app
        binding.ivBack.setOnClickListener { finish() }

        val notificationsAvailable = intent.getBooleanExtra("newNotificationsAvailable", false)
        userType = intent.getSerializableExtra("userType") as? UserType

        setupObservers()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // Fetch notifications and unread count
        //userType?.let { viewModel.fetchNotifications(it) }
        //viewModel.fetchUnreadCount(userType!!)
    }

    private fun setupObservers() {
        userType?.let { viewModel.fetchNotifications(it) }
        viewModel.notificationState.observe(this) { state ->
            when (state) {
                is NotificationActViewModel.NotificationState.Loading -> {
                    Log.d(TAG, "Loading")
                    binding.progressIndicator.visibility = View.VISIBLE
                    binding.layResult.visibility = View.GONE
                    binding.rcvNotiDate.visibility = View.GONE
                }

                is NotificationActViewModel.NotificationState.Success -> {
                    notificationList = state.notifications
                    Log.e(TAG, "onCreate: Size of Fetched Notification -> ${notificationList.size}")

                    if (notificationList.isEmpty()) {
                        binding.rcvNotiDate.visibility = View.GONE
                        binding.progressIndicator.visibility = View.GONE
                        binding.layResult.visibility = View.VISIBLE
                    } else {
                        binding.rcvNotiDate.visibility = View.VISIBLE
                        binding.progressIndicator.visibility = View.GONE
                        binding.layResult.visibility = View.GONE
                        setupRecyclerView(notificationList)

                        // Mark all notifications as read when they are fetched and displayed
                        viewModel.markAllNotificationsAsRead(userType!!)
                    }
                }

                is NotificationActViewModel.NotificationState.Error -> {
                    Log.e(TAG, "Error: ${state.message}")
                    binding.rcvNotiDate.visibility = View.GONE
                    binding.progressIndicator.visibility = View.GONE
                    binding.layResult.visibility = View.VISIBLE
                }
            }
        }

        // Observe unread count
        /*viewModel.unreadCount.observe(this) { count ->
            Log.d(TAG, "Unread notifications: $count")
            // You can update UI elements like badges, titles, etc.
            // For example: supportActionBar?.title = "Notifications ($count)"
        }*/

        // Observe mark as read state
        viewModel.markAsReadState.observe(this) { success ->
            if (success) {
                Log.d(TAG, "All notifications marked as read successfully")
            } else {
                Log.e(TAG, "Failed to mark notifications as read")
            }
        }

        // Observe create notification state
        viewModel.createNotificationState.observe(this) { success ->
            if (success) {
                Log.d(TAG, "Notification created successfully")
            } else {
                Log.e(TAG, "Failed to create notification")
            }
        }
    }

    private fun chipGroupListener() {
        binding.apply {
            chipGroup.setOnCheckedStateChangeListener { group, checkedId ->
                val filteredList = when (checkedId.firstOrNull()) {
                    chipAll.id -> notificationList/*chipReview.id -> notificationList.filter {
                        it.type == NotificationTypeLocal.REJECTED
                    }

                    chipReminders.id -> notificationList.filter {
                        it.type == NotificationTypeLocal.CHECKED_IN
                    }

                    chipPayout.id -> notificationList.filter {
                        it.type == NotificationTypeLocal.PAYMENT_REQUESTED
                    }*/

                    chipGame.id -> notificationList

                    chipGeneral.id -> emptyList()

                    else -> notificationList
                }

                notificationDatesAdapter.updateData(filteredList.groupBy {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.createdAt))
                }.map { (date, notifications) ->
                    NotificationDate(date, notifications)
                })
            }
        }
    }

    private fun setupRecyclerView(list: List<Notification>) {
        notificationDatesAdapter = NotificationDatesAdapter(this, list.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.createdAt))
        }.map { (date, notifications) ->
            NotificationDate(date, notifications)
        })
        binding.rcvNotiDate.adapter = notificationDatesAdapter
        chipGroupListener()
    }

    // Helper method to create a new notification (call this when needed)
    private fun createSampleNotification() {
        val notification = Notification(
            notificationId = "", // Will be generated automatically
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            userName = "John Doe",
            gameId = "game_123",
            gameName = "Football Match",
            refereeId = "referee_456",
            refereeName = "Referee Name",
            title = "Game Assignment",
            message = "You have been assigned to a new game",
            type = NotificationTypeLocal.PENDING,
            read = false,
            createdAt = System.currentTimeMillis()
        )

        viewModel.createNotification(notification)
    }


    override fun onDestroy() {
        super.onDestroy()
        // ViewModel will handle cleanup automatically through onCleared()
    }

    companion object {
        private const val TAG = "Notification_Activity"
    }
}