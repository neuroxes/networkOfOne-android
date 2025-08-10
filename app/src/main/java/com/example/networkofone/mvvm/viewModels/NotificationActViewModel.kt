package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.repo.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActViewModel() : ViewModel() {
    private val repository = NotificationRepository()

    sealed class NotificationState {
        object Loading : NotificationState()
        data class Success(val notifications: List<Notification>) : NotificationState()
        data class Error(val message: String) : NotificationState()
    }

    private val _notificationState = MutableLiveData<NotificationState>()
    val notificationState: LiveData<NotificationState> get() = _notificationState

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> get() = _unreadCount

    private val _markAsReadState = MutableLiveData<Boolean>()
    val markAsReadState: LiveData<Boolean> get() = _markAsReadState

    private val _createNotificationState = MutableLiveData<Boolean>()
    val createNotificationState: LiveData<Boolean> get() = _createNotificationState

    // Fetch notifications with real-time updates
    fun fetchNotifications(userType: UserType) {
        _notificationState.postValue(NotificationState.Loading)

        try {
            repository.getNotificationsRealtime(userType) { notifications ->
                if (notifications.isEmpty()) {
                    _notificationState.postValue(NotificationState.Success(emptyList()))
                } else {
                    _notificationState.postValue(
                        NotificationState.Success(notifications)
                    )
                }
            }
        } catch (e: Exception) {
            _notificationState.postValue(
                NotificationState.Error(e.message ?: "Unknown error occurred")
            )
        }
    }

    // Get unread notification count with real-time updates
    fun fetchUnreadCount(userType: UserType) {
        repository.getUnreadNotificationCountRealtime(userType) { count ->
            _unreadCount.postValue(count)
        }
    }

    // Create a new notification
    fun createNotification(notification: Notification) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.createNotification(notification)
                _createNotificationState.postValue(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification: ${e.message}")
                _createNotificationState.postValue(false)
            }
        }
    }

    // Mark all notifications as read
    fun markAllNotificationsAsRead(userType: UserType) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.markAllNotificationsAsRead(userType)
                _markAsReadState.postValue(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notifications as read: ${e.message}")
                _markAsReadState.postValue(false)
            }
        }
    }

    // Mark specific notification as read
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.markNotificationAsRead(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read: ${e.message}")
            }
        }
    }

    // Delete notification
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteNotification(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting notification: ${e.message}")
            }
        }
    }

    fun retry(userType: UserType) {
        fetchNotifications(userType)
    }

    // Clean up resources
    override fun onCleared() {
        super.onCleared()
        repository.removeNotificationListener()
    }

    companion object {
        private const val TAG = "NotificationActViewModel"
    }
}