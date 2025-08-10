package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.NotificationData
import com.example.networkofone.mvvm.models.NotificationType
import com.example.networkofone.mvvm.models.NotificationTypeLocal
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GameRepository {
    private val database =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val gamesRef = database.getReference("games")
    private val notificationRepo = NotificationRepositoryFirebase()
    private val notificationRepository = NotificationRepository()


    suspend fun saveGame(gameData: GameData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val gameId = gamesRef.push().key
                ?: return@withContext Result.failure(Exception("Failed to generate game ID"))
            val gameWithId = gameData.copy(id = gameId)

            gamesRef.child(gameId).setValue(gameWithId).await()

            // Send notification to all referees about new game
            notificationRepository.createNotification(
                Notification(
                    userId = gameWithId.createdBySchoolId,
                    userName = gameWithId.schedularName,
                    gameId = gameWithId.id,
                    gameName = gameWithId.title,
                    refereeId = gameWithId.acceptedByRefereeId.toString(),
                    title = "Game Posted",
                    message = "The game has been posted successfully.",
                    type = NotificationTypeLocal.PENDING,
                )
            )
            sendGamePostedNotification(gameWithId)

            Result.success(gameId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGame(gameData: GameData): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if the game ID exists (optional, but good for validation)
            if (gameData.id.isEmpty()) {
                Log.e("TAG", "updateGame: game id is empty!")
                return@withContext Result.failure(Exception("Game ID is missing"))
            }

            // Update the existing entry (overwrites only the specified fields if using updateChildren)
            gamesRef.child(gameData.id).setValue(gameData).await()
            notificationRepository.createNotification(
                Notification(
                    userId = gameData.createdBySchoolId,
                    userName = gameData.schedularName,
                    gameId = gameData.id,
                    gameName = gameData.title,
                    refereeId = gameData.acceptedByRefereeId.toString(),
                    title = "Game Updated",
                    message = "The detail has been updated successfully.",
                    type = NotificationTypeLocal.COMPLETED,
                )
            )
            // OR (for partial updates):
            // gamesRef.child(gameData.id).updateChildren(gameData.toMap()).await()

            Result.success(gameData.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGames(userId: String): Result<List<GameData>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = gamesRef.orderByChild("createdBy").equalTo(userId).get().await()
            val games = mutableListOf<GameData>()

            snapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(GameData::class.java)?.let { game ->
                    games.add(game)
                }
            }

            games.sortByDescending { it.createdAt }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAvailableGames(): Result<List<GameData>> = withContext(Dispatchers.IO) {
        try {
            val snapshot =
                gamesRef.orderByChild("status").equalTo(GameStatus.PENDING.name).get().await()
            val games = mutableListOf<GameData>()

            snapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(GameData::class.java)?.let { game ->
                    games.add(game)
                }
            }

            games.sortByDescending { it.createdAt }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptGame(gameId: String): Result<Unit> = try {
        val updates = mapOf<String, Any>(
            "status" to GameStatus.ACCEPTED,
            "acceptedByRefereeId" to userId,
            "acceptedAt" to System.currentTimeMillis()
        )
        gamesRef.child(gameId).updateChildren(updates).await()

        // Get game data and send notification to school
        val gameData = getGameById(gameId)
        gameData?.let { game ->
            sendGameAcceptedNotification(game)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectGame(gameId: String): Result<Unit> = try {
        val updates = mapOf<String, Any>(
            "status" to GameStatus.REJECTED
        )
        gamesRef.child(gameId).updateChildren(updates).await()

        // Get game data and send notification to school
        val gameData = getGameById(gameId)
        gameData?.let { game ->
            sendGameRejectedNotification(game)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun checkInToGame(gameId: String): Result<Unit> = try {
        val updates = mapOf<String, Any>(
            "status" to GameStatus.CHECKED_IN,
            "checkInStatus" to true,
            "checkInTime" to System.currentTimeMillis()
        )
        gamesRef.child(gameId).updateChildren(updates).await()

        // Get game data and send notification to school
        val gameData = getGameById(gameId)
        gameData?.let { game ->
            sendGameCheckedInNotification(game)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun sendGamePostedNotification(game: GameData) {
        // This would typically query all referee tokens
        // For now, sending to specific referees in your system
        val notification = NotificationData(
            title = "New Game Available",
            body = "${game.title} on ${game.date} at ${game.time} - Fee: $${game.feeAmount}",
            type = NotificationType.GAME_POSTED,
            targetUserId = "ALL_REFEREES", // Special case for broadcast
            gameId = game.id
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun sendGameAcceptedNotification(game: GameData) {
        val notification = NotificationData(
            title = "Game Accepted",
            body = "Your game '${game.title}' has been accepted by a referee",
            type = NotificationType.GAME_ACCEPTED,
            targetUserId = game.createdBySchoolId,
            gameId = game.id
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun sendGameRejectedNotification(game: GameData) {
        val notification = NotificationData(
            title = "Game Assignment Ended",
            body = "Assignment for '${game.title}' has ended and is available for other referees",
            type = NotificationType.GAME_REJECTED,
            targetUserId = game.createdBySchoolId,
            gameId = game.id
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun sendGameCheckedInNotification(game: GameData) {
        val notification = NotificationData(
            title = "Referee Checked In",
            body = "Referee has checked in for '${game.title}'",
            type = NotificationType.GAME_CHECKED_IN,
            targetUserId = game.createdBySchoolId,
            gameId = game.id
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun getGameById(id: String): GameData? {
        return try {
            val snapshot = gamesRef.child(id).get().await()
            snapshot.getValue(GameData::class.java)
        } catch (e: Exception) {
            Log.e("GameRepo", "Error fetching game $id", e)
            null
        }
    }
}


/*

class GameRepository {
    private val database = Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val gamesRef = database.getReference("games")

    suspend fun saveGame(gameData: GameData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val gameId = gamesRef.push().key
                ?: return@withContext Result.failure(Exception("Failed to generate game ID"))
            val gameWithId = gameData.copy(id = gameId)

            gamesRef.child(gameId).setValue(gameWithId).await()
            Result.success(gameId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateGame(gameData: GameData): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if the game ID exists (optional, but good for validation)
            if (gameData.id.isEmpty()) {
                Log.e("TAG", "updateGame: game id is empty!", )
                return@withContext Result.failure(Exception("Game ID is missing"))
            }

            // Update the existing entry (overwrites only the specified fields if using updateChildren)
            gamesRef.child(gameData.id).setValue(gameData).await()
            // OR (for partial updates):
            // gamesRef.child(gameData.id).updateChildren(gameData.toMap()).await()

            Result.success(gameData.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getUserGames(userId: String): Result<List<GameData>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = gamesRef.orderByChild("createdBy").equalTo(userId).get().await()
            val games = mutableListOf<GameData>()

            snapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(GameData::class.java)?.let { game ->
                    games.add(game)
                }
            }

            games.sortByDescending { it.createdAt }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAvailableGames(): Result<List<GameData>> = withContext(Dispatchers.IO) {
        try {
            val snapshot =
                gamesRef.orderByChild("status").equalTo(GameStatus.PENDING.name).get().await()
            val games = mutableListOf<GameData>()

            snapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(GameData::class.java)?.let { game ->
                    games.add(game)
                }
            }

            games.sortByDescending { it.createdAt }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
*/
