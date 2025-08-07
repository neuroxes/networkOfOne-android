package com.example.networkofone.mvvm.repo

import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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