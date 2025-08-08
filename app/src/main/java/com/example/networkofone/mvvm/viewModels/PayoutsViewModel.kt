package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.repo.PayoutsRepository

class PayoutsViewModel : ViewModel() {
    private val repository = PayoutsRepository()
    private val _uiState = MutableLiveData<PayoutsUiState>()
    val uiState: LiveData<PayoutsUiState> = _uiState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private var payoutsLiveData: LiveData<List<GameData>>? = null

    init {
        loadPayouts()
    }

    fun loadPayouts() {
        _uiState.value = PayoutsUiState.Loading

        payoutsLiveData = repository.getPayouts()
        payoutsLiveData?.observeForever { payouts ->
            when {
                payouts.isEmpty() -> _uiState.value = PayoutsUiState.Empty
                else -> _uiState.value = PayoutsUiState.Success(payouts)
            }
        }
    }

    fun searchPayouts(query: String) {
        _searchQuery.value = query
        _uiState.value = PayoutsUiState.Loading

        if (query.isBlank()) {
            loadPayouts()
            return
        }

        payoutsLiveData?.removeObserver { }
        payoutsLiveData = repository.searchPayouts(query)
        payoutsLiveData?.observeForever { payouts ->
            when {
                payouts.isEmpty() -> _uiState.value = PayoutsUiState.Empty
                else -> _uiState.value = PayoutsUiState.Success(payouts)
            }
        }
    }

    fun acceptPayout(gameId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        repository.acceptPayout(gameId) { success ->
            Log.e("", "acceptPayoutViewModel: $success" )
            result.value = success
        }
        return result
    }

    fun rejectPayout(gameId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        repository.rejectPayout(gameId) { success ->
            result.value = success
            // if (!success) {
            //     // Handle failure, e.g., show an error message
            // }
        }
        return result
    }
    override fun onCleared() {
        super.onCleared()
        payoutsLiveData?.removeObserver { }
    }
}


// 1. UI State Sealed Class
sealed class PayoutsUiState {
    object Loading : PayoutsUiState()
    object Empty : PayoutsUiState()
    data class Success(val payouts: List<GameData>) : PayoutsUiState()
    data class Error(val message: String) : PayoutsUiState()
}