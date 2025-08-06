package com.example.networkofone.mvvm.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.repo.GameRepository
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    
    private val _saveGameResult = MutableLiveData<Result<String>>()
    val saveGameResult: LiveData<Result<String>> = _saveGameResult
    
    private val _userGames = MutableLiveData<Result<List<GameData>>>()
    val userGames: LiveData<Result<List<GameData>>> = _userGames
    
    fun saveGame(gameData: GameData) {
        viewModelScope.launch {
            _saveGameResult.value = repository.saveGame(gameData)
        }
    }
    
    fun getUserGames(userId: String) {
        viewModelScope.launch {
            _userGames.value = repository.getUserGames(userId)
        }
    }
}

// 6. ViewModel Factory
class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}