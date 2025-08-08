package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.UserModel
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