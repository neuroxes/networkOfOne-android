package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.NotificationTypeLocal
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GameRepositoryImpl() {
    private val database: FirebaseDatabase =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val gamesRef = database.getReference("games")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val notificationRepository = NotificationRepository()

    // Original function to get all games
    fun getAllGames(): Flow<List<GameData>> = callbackFlow {
        val listener = object : ValueEventListener {
            var dataFetched = false
            override fun onDataChange(snapshot: DataSnapshot) {
                dataFetched = true
                val games = mutableListOf<GameData>()
                snapshot.children.forEach { child ->
                    child.getValue<GameData>()?.let { game ->
                        games.add(game)
                    }
                }
                trySend(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                dataFetched = true
                close(error.toException())
            }
        }

        gamesRef.addValueEventListener(listener)

        val timeoutJob = launch {
            delay(5000L)
            if (!isClosedForSend && !listener.dataFetched) {
                close(Exception("Timed out waiting for data"))
            }
        }

        awaitClose { gamesRef.removeEventListener(listener); timeoutJob.cancel() }
    }

    fun getAvailableGamesForReferee(): Flow<List<GameData>> = callbackFlow {
        val listener = object : ValueEventListener {
            var dataFetched = false
            override fun onDataChange(snapshot: DataSnapshot) {
                dataFetched = true
                val games = mutableListOf<GameData>()
                snapshot.children.forEach { child ->
                    child.getValue<GameData>()?.let { game ->
                        // Only include games that are either unassigned or assigned to this referee
                        if (game.acceptedByRefereeId.isNullOrEmpty() || game.acceptedByRefereeId == userId) {
                            games.add(game)
                        }
                    }
                }
                trySend(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                dataFetched = true
                close(error.toException())
            }
        }

        gamesRef.addValueEventListener(listener)

        val timeoutJob = launch {
            delay(5000L)
            if (!isClosedForSend && !listener.dataFetched) {
                close(Exception("Timed out waiting for data"))
            }
        }

        awaitClose { gamesRef.removeEventListener(listener); timeoutJob.cancel() }
    }

    // New function to get games by creator ID
    fun getGamesByCreator(): Flow<List<GameData>> = callbackFlow {
        val listener = object : ValueEventListener {
            var dataFetched = false
            override fun onDataChange(snapshot: DataSnapshot) {
                dataFetched = true
                val games = mutableListOf<GameData>()
                snapshot.children.forEach { child ->
                    child.getValue<GameData>()?.let { game ->
                        if (game.createdBySchoolId == userId) {
                            games.add(game)
                        }
                    }
                }
                trySend(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                dataFetched = true
                close(error.toException())
            }
        }

        gamesRef.addValueEventListener(listener)

        val timeoutJob = launch {
            delay(5000L)
            if (!isClosedForSend && !listener.dataFetched) {
                close(Exception("Timed out waiting for data"))
            }
        }

        awaitClose { gamesRef.removeEventListener(listener); timeoutJob.cancel() }
    }

    // More efficient version using Firebase query
    fun getGamesByCreatorOptimized(): Flow<List<GameData>> = callbackFlow {
        val listener = object : ValueEventListener {
            var dataFetched = false
            override fun onDataChange(snapshot: DataSnapshot) {
                dataFetched = true
                val games = mutableListOf<GameData>()
                snapshot.children.forEach { child ->
                    child.getValue<GameData>()?.let { game ->
                        games.add(game)
                    }
                }
                trySend(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                dataFetched = true
                close(error.toException())
            }
        }

        // Use Firebase query to filter on the server side
        gamesRef.orderByChild("createdBySchoolId").equalTo(userId).addValueEventListener(listener)

        val timeoutJob = launch {
            delay(5000L)
            if (!isClosedForSend && !listener.dataFetched) {
                close(Exception("Timed out waiting for data"))
            }
        }

        awaitClose {
            gamesRef.orderByChild("createdBySchoolId").equalTo(userId).removeEventListener(listener)
            timeoutJob.cancel()
        }
    }

    suspend fun getGameById(id: String): GameData? {
        return try {
            val snapshot = gamesRef.child(id).get().await()
            snapshot.getValue(GameData::class.java).also {
                if (it == null) {
                    Log.e("Firebase", "Payout with ID $id not found or parsing failed")
                }
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error fetching payout $id", e)
            null
        }
    }

    suspend fun updateGame(game: GameData): Result<Unit> = try {
        gamesRef.child(game.id).setValue(game).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateGame(game: GameData, status: GameStatus): Result<Unit> = try {
        // Create a map of the fields to update
        Log.e("TAG", "updateGame in GamRepImp: $game")
        val updates = when (status) {
            GameStatus.ACCEPTED -> {
                mapOf<String, Any>(
                    "status" to status,
                    "acceptedByRefereeId" to userId,
                    "refereeName" to game.refereeName!!,
                    "acceptedAt" to System.currentTimeMillis()
                )
            }

            GameStatus.PENDING -> {
                mapOf<String, Any?>(
                    "status" to status,
                    "acceptedByRefereeId" to null,
                    "refereeName" to null,
                    "acceptedAt" to null,
                    "checkInStatus" to false,
                    "checkInTime" to null
                )
            }

            else -> {
                mapOf<String, Any>(
                    "status" to status,
                    "checkInStatus" to true,
                    "checkInTime" to System.currentTimeMillis()
                )
            }
        }
        Log.e("TAG", "updateGame in GameRepoImp try: after updates assignment")
        gamesRef.child(game.id).updateChildren(updates).await()

        // Create enhanced notification with detailed message and appropriate type
        val (notificationMessage, notificationType, notificationTitle) = createNotificationContent(
            game, status
        )

        notificationRepository.createNotification(
            Notification(
                userId = game.createdBySchoolId,
                userName = game.schedularName,
                gameId = game.id,
                gameName = game.title,
                refereeId = userId,
                refereeName = game.refereeName ?: "Unknown Referee",
                title = notificationTitle,
                message = notificationMessage,
                type = notificationType,
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("TAG", "updateGame in GameRepoImp catch: ${e.message}")
        Result.failure(e)
    }

    private fun createNotificationContent(
        game: GameData,
        newStatus: GameStatus,
    ): Triple<String, NotificationTypeLocal, String> {
        val gameInfo = buildGameInfoString(game)
        val statusChangeInfo =
            "Status changed from ${formatStatus(game.status)} to ${formatStatus(newStatus)}"

        return when (newStatus) {
            GameStatus.ACCEPTED -> {
                val message =
                    "Great news! Game \"${game.title}\" has been accepted by referee ${game.refereeName}.\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "The referee will be present at the scheduled time. Please ensure all preparations are complete."

                Triple(message, NotificationTypeLocal.ACCEPTED, "Game Accepted")
            }

            GameStatus.PENDING -> {
                val message =
                    "Your game \"${game.title}\" status has been updated to pending.\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "The game is now waiting for referee assignment. Will be notified once a referee accepts the game."

                Triple(message, NotificationTypeLocal.PENDING, "Game Status Updated")
            }

            GameStatus.REJECTED -> {
                val message =
                    "Unfortunately, your game \"${game.title}\" payout has been rejected.\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "Please review the game details and consider reposting with updated information or contact support for assistance."

                Triple(message, NotificationTypeLocal.REJECTED, "Game Rejected")
            }

            GameStatus.CHECKED_IN -> {
                val currentTime = SimpleDateFormat(
                    "hh:mm a", Locale.getDefault()
                ).format(System.currentTimeMillis())
                val message =
                    "Referee ${game.refereeName} has checked in for game \"${game.title}\" at $currentTime.\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "The referee is now present at the venue. The game can proceed as scheduled."

                Triple(message, NotificationTypeLocal.CHECKED_IN, "Referee Checked In")
            }

            GameStatus.PAYMENT_REQUESTED -> {
                val message =
                    "Payment has been requested for your game \"${game.title}\".\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "<br>Please process the payment to complete the transaction. The referee has fulfilled their duties."

                Triple(message, NotificationTypeLocal.PAYMENT_REQUESTED, "Payment Required")
            }

            GameStatus.COMPLETED -> {
                val message =
                    "Your game \"${game.title}\" has been successfully completed!\n\n" + "$statusChangeInfo\n\n$gameInfo\n\n" + "Thank you for using our service. We hope you had a great game experience. " + "Please consider leaving a review for the referee."

                Triple(message, NotificationTypeLocal.COMPLETED, "Game Completed")
            }
        }
    }

    private fun buildGameInfoString(game: GameData): String {
        val gameDetails = mutableListOf<String>()

        gameDetails.add("\n<br>📅 Date: ${game.date}")
        gameDetails.add("\n<br>⏰ Time: ${game.time}")
        gameDetails.add("\n<br>📍 Location: ${game.location}")

        if (game.feeAmount.isNotEmpty()) {
            gameDetails.add("\n<br>💰 Fee: $${game.feeAmount}")
        }

        if (game.refereeName?.isNotEmpty() == true) {
            gameDetails.add("\n<br>👨‍⚖️ Referee: ${game.refereeName}")
        }

        if (game.specialNote.isNotEmpty()) {
            gameDetails.add("\n<br>📝 Note: ${game.specialNote}")
        }

        return gameDetails.joinToString("\n")
    }

    private fun formatStatus(status: GameStatus): String {
        return when (status) {
            GameStatus.PENDING -> "Pending"
            GameStatus.ACCEPTED -> "Accepted"
            GameStatus.REJECTED -> "Payout Rejected"
            GameStatus.CHECKED_IN -> "Checked In"
            GameStatus.PAYMENT_REQUESTED -> "Payout Requested"
            GameStatus.COMPLETED -> "Completed"
        }
    }

    suspend fun updateGame(payout: PaymentRequestData, status: GameStatus): Result<Unit> = try {
        // Create a map of the fields to update

        val updates = if (status == GameStatus.ACCEPTED) {
            mapOf<String, Any>(
                "status" to status,
                "acceptedByRefereeId" to userId,
                "acceptedAt" to System.currentTimeMillis()
            )
        } else {
            mapOf<String, Any>(
                "status" to status,
                "checkInStatus" to true,
                "checkInTime" to System.currentTimeMillis()
            )
        }
        gamesRef.child(payout.gameId).updateChildren(updates).await()
        notificationRepository.createNotification(
            Notification(
                // The user to whom this notification is being sent.
                userId = payout.schedularId, // This is the ID of the game scheduler/creator.
                userName = payout.schedularName, // The name of the game scheduler/creator.
                gameId = payout.gameId,
                gameName = payout.gameName,
                refereeId = userId, // The ID of the referee involved in this game update.
                title = "Game Updated",
                // A descriptive message for the notification.
                message = "The status of game \"${payout.gameName}\" has been updated to \"$status\".",
                // The type of notification, in this case, it's related to a pending action or status update.
                type = NotificationTypeLocal.PENDING,
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("TAG", "updateGame in GameRepoImp catch: ${e.message}")
        Result.failure(e)
    }

    suspend fun deleteGame(gameId: String): Result<Unit> = try {
        gamesRef.child(gameId).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }


    suspend fun createPaymentRequest(paymentRequestData: PaymentRequestData): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val paymentRequestRef = database.getReference("paymentRequests")

                /*// First query by refereeId (more efficient than getting all records)
                val query = paymentRequestRef.orderByChild("refereeId").equalTo(paymentRequestData.refereeId)
                val snapshot = query.get().await()

                // Then check gameId in matching records
                snapshot.children.forEach { child ->
                    val existingGameId = child.child("gameId").getValue(String::class.java)
                    if (existingGameId == paymentRequestData.gameId) {
                        return@withContext Result.failure(Exception("Duplicate payment request exists"))
                    }
                }*/

                val id = paymentRequestRef.push().key
                    ?: return@withContext Result.failure(Exception("ID generation failed"))
                paymentRequestRef.child(id).setValue(paymentRequestData.copy(id = id)).await()

                notificationRepository.createNotification(
                    Notification(
                        userId = paymentRequestData.schedularId,
                        userName = paymentRequestData.schedularName,
                        gameId = paymentRequestData.gameId,
                        gameName = paymentRequestData.gameName,
                        refereeId = paymentRequestData.refereeId,
                        refereeName = paymentRequestData.refereeName,
                        title = "Payout Requested",
                        message = "Dear user, referee \"${paymentRequestData.refereeName}\" has requested payment of $${paymentRequestData.amount} for game \"${paymentRequestData.gameName}\".",
                        type = NotificationTypeLocal.PAYMENT_REQUESTED
                    )
                )
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

}
