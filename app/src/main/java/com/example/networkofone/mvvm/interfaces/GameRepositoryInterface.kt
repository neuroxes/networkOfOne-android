package com.example.networkofone.mvvm.interfaces

import com.example.networkofone.mvvm.models.GameData
import kotlinx.coroutines.flow.Flow

interface GameRepositoryInterface {
    fun getAllGames(): Flow<List<GameData>>
    suspend fun updateGame(game: GameData): Result<Unit>
    suspend fun deleteGame(gameId: String): Result<Unit>
}