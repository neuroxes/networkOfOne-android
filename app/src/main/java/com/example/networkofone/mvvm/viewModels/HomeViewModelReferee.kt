package com.example.networkofone.mvvm.viewModels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModelReferee(
    private val repository: GameRepositoryImpl,
) : ViewModel() {

    private val _uiState = MutableLiveData<GameUiState>(GameUiState.Loading)
    val uiState: LiveData<GameUiState> = _uiState

    private val _selectedTab =
        MutableLiveData(0) // 0: All, 1: Recent, 2: Active, 3: Payout Pending, 4: Completed
    val selectedTab: LiveData<Int> = _selectedTab

    private val _filteredGames = MutableLiveData<List<GameData>>()
    val filteredGames: LiveData<List<GameData>> = _filteredGames

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _paymentRequestResult = MutableLiveData<Result<String>>()
    val paymentRequestResult: LiveData<Result<String>> = _paymentRequestResult

    private var allGames: List<GameData> = emptyList()

    var checkInGame: GameData? = null


    init {
        observeGames()
    }


    fun observeGames() {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            repository.getAvailableGamesForReferee().catch { exception ->
                _uiState.value = GameUiState.Error(
                    exception.message ?: "An error occurred while loading games"
                )
            }.collect { games ->
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
            0 -> allGames.filter { it.status == GameStatus.PENDING} // All
            /*1 -> { // Recent - games from last 7 days
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                allGames.filter { it.createdAt >= sevenDaysAgo }
            }*/

            1 -> allGames.filter { it.status == GameStatus.ACCEPTED || it.status == GameStatus.CHECKED_IN } // Active
            /*3 -> allGames.filter { it.status == GameStatus.CHECKED_IN } // Completed*/
            2 -> allGames.filter { it.status == GameStatus.COMPLETED } // Payout Pending
            else -> allGames
        }
        _filteredGames.value = filtered
    }

    fun updateGame(game: GameData, status: GameStatus) {
        viewModelScope.launch {
            _updateResult.value = repository.updateGame(game, status)
        }
    }
    fun updateGame(payout: PaymentRequestData, status: GameStatus) {
        viewModelScope.launch {
            _updateResult.value = repository.updateGame(payout, status)
        }
    }

    fun createPaymentRequest(paymentRequestData: PaymentRequestData) {
        try {
            viewModelScope.launch {
                _paymentRequestResult.value = repository.createPaymentRequest(paymentRequestData)
                updateGame(paymentRequestData, GameStatus.PAYMENT_REQUESTED)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPaymentRequest: ${e.message}")
        }
    }

}


class HomeViewModelRefereeFactory(
    private val repository: GameRepositoryImpl,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModelReferee::class.java)) {
            @Suppress("UNCHECKED_CAST") return HomeViewModelReferee(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}