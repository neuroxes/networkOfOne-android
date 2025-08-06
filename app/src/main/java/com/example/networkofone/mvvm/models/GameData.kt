package com.example.networkofone.mvvm.models
// 2. Game Status Enum

// 3. Game Data Model
data class GameData(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val date: String = "",
    val time: String = "",
    val feeAmount: String = "",
    val specialNote: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: GameStatus = GameStatus.PENDING,
    val acceptedBy: String? = null,
    val acceptedAt: Long? = null
)

enum class GameStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED
}
