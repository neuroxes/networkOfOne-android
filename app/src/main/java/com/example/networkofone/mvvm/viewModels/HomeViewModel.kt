package com.example.networkofone.mvvm.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.interfaces.GameRepositoryInterface
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: GameRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableLiveData<GameUiState>(GameUiState.Loading)
    val uiState: LiveData<GameUiState> = _uiState

    private val _selectedTab = MutableLiveData(0) // 0: All, 1: Recent, 2: Active, 3: Payout Pending, 4: Completed
    val selectedTab: LiveData<Int> = _selectedTab

    private val _filteredGames = MutableLiveData<List<GameData>>()
    val filteredGames: LiveData<List<GameData>> = _filteredGames

    private var allGames: List<GameData> = emptyList()



    init {
        observeGames()
    }


    fun observeGames() {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            repository.getAllGames()
                .catch { exception ->
                    _uiState.value = GameUiState.Error(
                        exception.message ?: "An error occurred while loading games"
                    )
                }
                .collect { games ->
                    allGames = games
                    if (games.isEmpty()) {
                        _uiState.value = GameUiState.Empty
                    } else {
                        _uiState.value = GameUiState.Success(games)
                        applyFilter(_selectedTab.value ?: 0)
                    }
                }
        }
    }

    fun onTabSelected(position: Int) {
        _selectedTab.value = position
        applyFilter(position)
    }

    private fun applyFilter(tabPosition: Int) {
        val filtered = when (tabPosition) {
            0 -> allGames // All
            1 -> { // Recent - games from last 7 days
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                allGames.filter { it.createdAt >= sevenDaysAgo }
            }
            2 -> allGames.filter { it.status == GameStatus.ACCEPTED } // Active
            3 -> allGames.filter { it.status == GameStatus.COMPLETED } // Completed
//            3 -> allGames.filter { it.status == GameStatus.PENDING } // Payout Pending
            else -> allGames
        }
        _filteredGames.value = filtered
    }

    fun editGame(game: GameData) {
        viewModelScope.launch {
            repository.updateGame(game).fold(
                onSuccess = {
                    // Game updated successfully
                },
                onFailure = { exception ->
                    // Handle error - could show toast or snackbar
                }
            )
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            repository.deleteGame(gameId).fold(
                onSuccess = {
                    // Game deleted successfully
                },
                onFailure = { exception ->
                    // Handle error - could show toast or snackbar
                }
            )
        }
    }
}

sealed class GameUiState {
    object Loading : GameUiState()
    data class Success(val games: List<GameData>) : GameUiState()
    data class Error(val message: String) : GameUiState()
    object Empty : GameUiState()
}


class HomeViewModelFactory(
    private val repository: GameRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}