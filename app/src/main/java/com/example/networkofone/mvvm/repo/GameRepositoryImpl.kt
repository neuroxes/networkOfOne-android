package com.example.networkofone.mvvm.repo

import com.example.networkofone.mvvm.interfaces.GameRepositoryInterface
import com.example.networkofone.mvvm.models.GameData
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue

import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await



class GameRepositoryImpl(){
    private val database: FirebaseDatabase = Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val gamesRef = database.getReference("games")

    fun getAllGames(): Flow<List<GameData>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val games = mutableListOf<GameData>()
                snapshot.children.forEach { child ->
                    child.getValue<GameData>()?.let { game ->
                        games.add(game)
                    }
                }
                trySend(games.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        gamesRef.addValueEventListener(listener)
        awaitClose { gamesRef.removeEventListener(listener) }
    }

    suspend fun updateGame(game: GameData): Result<Unit> = try {
        gamesRef.child(game.id).setValue(game).await()
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
