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
    var refereeName: String? = null,
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
        } catch (_: Exception) {
        }
    }
    return formatter.format(this)
}

/*
[PaymentRequestData(id=-OXCkQ7eZRiSw0QW2XKo, gameId=-OXCiwxqORy7qayF6VYb, gameName=, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=N1ziEyAV3uXVGIa98maOoeWhvT33, schedularName=Ali Hassan School, amount=2009, paymentMethod=VENMO, status=REJECTED, requestedAt=1754724974508, paidAt=null, transactionId=null)
 PaymentRequestData(id=-OXCzWFHoi_b6LyBDOEI, gameId=-OXCj0_AxBgsgqZkc-OP, gameName=Game 3, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=N1ziEyAV3uXVGIa98maOoeWhvT33, schedularName=Ali Hassan School, amount=6000, paymentMethod=BANK_TRANSFER, status=APPROVED, requestedAt=1754728931738, paidAt=1754729257966, transactionId=null),
PaymentRequestData(id=-OXD09lES9Wb_SFFfXRh, gameId=-OXCiwxqORy7qayF6VYb, gameName=Game 2, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=N1ziEyAV3uXVGIa98maOoeWhvT33, schedularName=Ali Hassan School, amount=2009, paymentMethod=PAYPAL, status=REJECTED, requestedAt=1754729363937, paidAt=null, transactionId=null),
PaymentRequestData(id=-OXDBlSkYt8cC-sTYxFD, gameId=-OXChl9Cnxzkge2MjD8x, gameName=Ali game, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=N1ziEyAV3uXVGIa98maOoeWhvT33, schedularName=Ali Hassan School, amount=300, paymentMethod=PAYPAL, status=APPROVED, requestedAt=1754732406039, paidAt=1754732390691, transactionId=null),
PaymentRequestData(id=-OXDF1QPzZxF7iKzVBIJ, gameId=-OXDE9Bet2w2iiq7r7RM, gameName=Game 2, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=olABliMMeAP8lMTtQjNvJzGXZXX2, schedularName=My Accoun Referee, amount=300, paymentMethod=PAYPAL, status=APPROVED, requestedAt=1754733262018, paidAt=1754733249541, transactionId=null),
PaymentRequestData(id=-OXDGWCYfW2-tmm9elkJ, gameId=-OXDDsEppgLECSqqjAaK, gameName=Game 1, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=olABliMMeAP8lMTtQjNvJzGXZXX2, schedularName=My Accoun Referee, amount=1000, paymentMethod=PAYPAL, status=REJECTED, requestedAt=1754733650169, paidAt=null, transactionId=null),
PaymentRequestData(id=-OXIgA6n1SXQB19kVh41, gameId=-OXHmWMbWoZPfL4wRVX2, gameName=Notifications Game, refereeId=f9edrjQpbhgSAKRK8jVhRvccBh13, refereeName=Referee, schedularId=olABliMMeAP8lMTtQjNvJzGXZXX2, schedularName=My Accoun Referee, amount=600.63, paymentMethod=PAYPAL, status=APPROVED, requestedAt=1754824523660, paidAt=1754833161733, transactionId=null),
PaymentRequestData(id=-OXKVYZZeVwlqPsmy03W, gameId=-OXKIt4Igq6IPGhXxch5, gameName=Saad's Game 1, refereeId=EYR9ihmfjJYcCei1sLxlOr7R0W43, refereeName=Usman Refree, schedularId=1ReMBZu5qOgaYHJZmcZypuf71rJ3, schedularName=Saad Khalil, amount=113, paymentMethod=XRPL, status=APPROVED, requestedAt=1754854995178, paidAt=1754855674620, transactionId=null),
PaymentRequestData(id=-OXK_EAE1ZFQjVKw1Uro, gameId=-OXKIt4Igq6IPGhXxch5, gameName=Saad's Game 1 Update, refereeId=EYR9ihmfjJYcCei1sLxlOr7R0W43, refereeName=Usman Refree, schedularId=1ReMBZu5qOgaYHJZmcZypuf71rJ3, schedularName=Saad Khalil, amount=113, paymentMethod=XRPL, status=APPROVED, requestedAt=1754856222503, paidAt=1754856312234, transactionId=null),
PaymentRequestData(id=-OXKbuLFbgpXHzeR5qpy, gameId=-OXKa_wxrd45ebmZ7te_, gameName=456, refereeId=EYR9ihmfjJYcCei1sLxlOr7R0W43, refereeName=Usman Refree, schedularId=1ReMBZu5qOgaYHJZmcZypuf71rJ3, schedularName=Saad Khalil, amount=999, paymentMethod=XRPL, status=APPROVED, requestedAt=1754856923586, paidAt=1754857162407, transactionId=null)]:
14:54:14.941 TAG               E*/
