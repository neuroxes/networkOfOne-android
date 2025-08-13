package com.example.networkofone.mvvm.viewModels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import kotlinx.coroutines.launch

class GameDetailActivityViewModel : ViewModel() {

    private var _gameData = GameData()
    private val repository = GameRepositoryImpl()

    // Game data LiveData with better naming
    private val _gameDataLiveData = MutableLiveData<GameData>()
    val gameDataLiveData: LiveData<GameData> = _gameDataLiveData

    // Loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Update result
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    // Payment request result
    private val _paymentRequestResult = MutableLiveData<Result<String>>()
    val paymentRequestResult: LiveData<Result<String>> = _paymentRequestResult

    /**
     * Set game data directly (when passed from intent)
     */
    fun setGameData(gameData: GameData) {
        _gameData = gameData.copy()
        _gameDataLiveData.value = _gameData
    }

    /**
     * Get current game data (defensive copy)
     */
    fun getGameData(): GameData {
        return _gameData.copy()
    }

    /**
     * Fetch game data from repository
     */
    fun getData(id: String) {
        if (id.isEmpty()) {
            _error.value = "Invalid game ID"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = "" // Clear previous errors

                val gameData = repository.getGameById(id)
                if (gameData != null) {
                    _gameData = gameData.copy()
                    _gameDataLiveData.value = _gameData
                } else {
                    _error.value = "Game not found"
                    _gameDataLiveData.value = GameData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching game data: ${e.message}")
                _error.value = "Failed to load game data: ${e.message}"
                _gameDataLiveData.value = GameData()
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update game status
     */
    fun updateGame(newStatus: GameStatus, userName: String? = null) {
        if (_gameData.id.isEmpty()) {
            Log.e(TAG, "GameDetViewModel: Game data not available")
            _updateResult.value = Result.failure(Exception("Game data not available"))
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = ""

                Log.e(TAG, "Updating game ${_gameData.id} from ${_gameData.status} to $newStatus")

                val result: Result<Unit> = if (userName != null) repository.updateGame(
                    _gameData.copy(refereeName = userName),
                    newStatus
                )
                else repository.updateGame(_gameData, newStatus)

                Log.e(TAG, "updateGame inside try: $result")
                if (result.isSuccess) {
                    val updatedGame = _gameData.copy()
                    // Update local data on successful update
                    _gameData = updatedGame.copy()
                    // Note: We don't update _gameDataLiveData here because we want to
                    // refresh from server to get the latest data
                }

                _updateResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error updating game: ${e.message}")
                _updateResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update local game status without server call (for UI updates)
     */
    fun updateLocalGameStatus(newStatus: GameStatus) {
        _gameData = _gameData.copy(status = newStatus)
        _gameDataLiveData.value = _gameData
    }

    /**
     * Create payment request
     */
    fun createPaymentRequest(paymentRequestData: PaymentRequestData) {
        // Validate payment request data
        if (paymentRequestData.gameId.isEmpty() || paymentRequestData.refereeId.isEmpty() || paymentRequestData.amount.toDoubleOrNull()!! <= 0.0) {
            _paymentRequestResult.value = Result.failure(
                Exception("Invalid payment request data")
            )
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = ""

                Log.d(TAG, "Creating payment request for game ${paymentRequestData.gameId}")

                val result = repository.createPaymentRequest(paymentRequestData)
                repository.updateGame(_gameData, GameStatus.PAYMENT_REQUESTED)
                _paymentRequestResult.value = result

                if (result.isFailure) {
                    Log.e(TAG, "Payment request failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payment request: ${e.message}")
                _paymentRequestResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Check if game data is valid
     */
    fun isGameDataValid(): Boolean {
        return _gameData.id.isNotEmpty() && _gameData.title.isNotEmpty()
    }

    /**
     * Get current game status
     */
    fun getCurrentStatus(): GameStatus {
        return _gameData.status
    }

    /**
     * Check if user can perform check-in
     */
    fun canCheckIn(): Boolean {
        return _gameData.status == GameStatus.ACCEPTED
    }

    /**
     * Check if user can request payment
     */
    fun canRequestPayment(): Boolean {
        return _gameData.status == GameStatus.CHECKED_IN
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}