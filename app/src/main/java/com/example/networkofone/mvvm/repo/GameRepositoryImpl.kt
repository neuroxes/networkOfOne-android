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

        val updates = if (status == GameStatus.ACCEPTED) {
            mapOf<String, Any>(
                "status" to status,
                "acceptedByRefereeId" to userId,
                "refereeName" to game.refereeName!!,
                "acceptedAt" to System.currentTimeMillis()
            )
        } else {
            mapOf<String, Any>(
                "status" to status,
                "checkInStatus" to true,
                "checkInTime" to System.currentTimeMillis()
            )
        }
        gamesRef.child(game.id).updateChildren(updates).await()
        notificationRepository.createNotification(
            Notification(
                userId = game.createdBySchoolId,
                userName = game.schedularName,
                gameId = game.id,
                gameName = game.title,
                refereeId = game.acceptedByRefereeId.toString(),
                title = "Game Updated",
                message = "The status of game \"${game.title}\" has changed from \"${game.status}\" to \"$status\".",
                type = NotificationTypeLocal.PENDING,
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
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
                refereeId = payout.refereeId, // The ID of the referee involved in this game update.
                title = "Game Updated",
                // A descriptive message for the notification.
                message = "The status of game \"${payout.gameName}\" has been updated to \"$status\".",
                // The type of notification, in this case, it's related to a pending action or status update.
                type = NotificationTypeLocal.PENDING,
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteGame(gameId: String): Result<Unit> = try {
        gamesRef.child(gameId).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }


    suspend fun createPaymentRequest(paymentRequestData: PaymentRequestData): Result<String> = withContext(Dispatchers.IO) {
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

            val id = paymentRequestRef.push().key ?: return@withContext Result.failure(Exception("ID generation failed"))
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
