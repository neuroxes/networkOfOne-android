package com.example.networkofone.mvvm.models
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
    var createdBySchoolId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var status: GameStatus = GameStatus.PENDING,
    var acceptedByRefereeId: String? = null,
    var acceptedAt: Long? = null,
    var checkInStatus: Boolean = false,
    var checkInTime: Long? = null
)

enum class GameStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CHECKED_IN,
    COMPLETED
}

data class PaymentRequestData(
    val id: String = "",
    val gameId: String = "",
    val refereeId: String = "",
    val schoolId: String = "",
    val amount: String = "",
    val paymentMethod: String = "", // e.g., "Bank Transfer", "Cash", etc.
    val status: PaymentStatus = PaymentStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val transactionId: String? = null
)


enum class PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    PAID
}
