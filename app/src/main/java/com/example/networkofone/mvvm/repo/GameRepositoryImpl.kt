package com.example.networkofone.mvvm.repo

import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameRepositoryImpl() {
    private val database: FirebaseDatabase =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val gamesRef = database.getReference("games")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
                        if (game.acceptedByRefereeId.isNullOrEmpty() ||
                            game.acceptedByRefereeId == userId) {
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
        gamesRef.orderByChild("createdBySchoolId")
            .equalTo(userId)
            .addValueEventListener(listener)

        val timeoutJob = launch {
            delay(5000L)
            if (!isClosedForSend && !listener.dataFetched) {
                close(Exception("Timed out waiting for data"))
            }
        }

        awaitClose {
            gamesRef.orderByChild("createdBySchoolId")
                .equalTo(userId)
                .removeEventListener(listener)
            timeoutJob.cancel()
        }
    }

    suspend fun updateGame(game: GameData): Result<Unit> = try {
        gamesRef.child(game.id).setValue(game).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateGame(gameId: String, status: GameStatus): Result<Unit> = try {
        // Create a map of the fields to update

        val updates = if(status== GameStatus.ACCEPTED){
            mapOf<String, Any>(
                "status" to status,
                "acceptedByRefereeId" to userId,
                "acceptedAt" to System.currentTimeMillis()
            )
        } else{
            mapOf<String, Any>(
                "status" to status,
                "checkInStatus" to true,
                "checkInTime" to System.currentTimeMillis()
            )
        }
        gamesRef.child(gameId).updateChildren(updates).await()
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

}
