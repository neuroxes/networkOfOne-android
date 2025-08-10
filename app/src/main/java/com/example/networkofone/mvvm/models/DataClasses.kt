package com.example.networkofone.mvvm.models

import androidx.room.Entity
import java.text.NumberFormat
import java.util.Locale

// 2. Game Status Enum
data class GameData(
    var id: String = "",
    val title: String = "",
    val location: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    val date: String = "",
    val time: String = "",
    val feeAmount: String = "",
    val specialNote: String = "",
    var schedularName: String = "",
    var createdBySchoolId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var status: GameStatus = GameStatus.PENDING,
    var acceptedByRefereeId: String? = null,
    var acceptedAt: Long? = null,
    var checkInStatus: Boolean = false,
    var checkInTime: Long? = null,
)

enum class GameStatus {
    PENDING, ACCEPTED, REJECTED, CHECKED_IN, PAYMENT_REQUESTED, COMPLETED
}

data class PaymentRequestData(
    val id: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val refereeId: String = "",
    val refereeName: String = "",
    val schedularId: String = "",
    val schedularName: String = "",
    val amount: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.NONE,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val transactionId: String? = null,
)


enum class PaymentStatus {
    PENDING, APPROVED, REJECTED, PAID
}

enum class PaymentMethod {
    XRPL, BANK_TRANSFER, PAYPAL, VENMO, NONE // Default or placeholder
}

// 1. Notification Data Classes
data class NotificationData(
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetUserId: String,
    val gameId: String? = null,
    val paymentRequestId: String? = null,
    val additionalData: Map<String, String> = emptyMap(),
)

enum class NotificationType {
    GAME_POSTED, GAME_ACCEPTED, GAME_REJECTED, GAME_CHECKED_IN, PAYMENT_REQUESTED, PAYMENT_APPROVED, PAYMENT_REJECTED
}

data class FCMToken(
    val userId: String = "",
    val token: String = "",
    val userType: UserType = UserType.UNKNOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)

enum class UserType {
    SCHOOL, REFEREE, UNKNOWN, ADMIN
}



data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val userName: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val refereeId: String = "",
    val refereeName: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationTypeLocal? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notification_types")
enum class NotificationTypeLocal {
    ACCEPTED, PENDING, PAYMENT_REQUESTED, REJECTED, CHECKED_IN, COMPLETED
}


data class NotificationDate(
    val date: String,
    val notifications: List<Notification>,
)


data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Live System Metrics
    val totalGames: Int = 0,
    val totalPayoutsCount: Int = 0, // number of paid payment requests

    // Games Analytics
    val gamesPending: Int = 0,
    val gamesAccepted: Int = 0,
    val gamesCompleted: Int = 0,
    val gamesCancelled: Int = 0,

    // Payout Analytics
    val payoutTotalValue: Double = 0.0,  // sum of PAID amounts
    val payoutPendingCount: Int = 0,     // PENDING or APPROVED
    val payoutCompletedCount: Int = 0,   // PAID
    val payoutAverageAmount: Double = 0.0,

    // System Health (simple heuristics)
    val realTimeSyncOk: Boolean = false,
    val dataIntegrityOk: Boolean = true,
)

fun Double.asCurrency(locale: Locale = Locale.US, currencyCode: String? = "USD"): String {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    if (currencyCode != null) {
        try {
            val cur = java.util.Currency.getInstance(currencyCode)
            formatter.currency = cur
        } catch (_: Exception) {}
    }
    return formatter.format(this)
}