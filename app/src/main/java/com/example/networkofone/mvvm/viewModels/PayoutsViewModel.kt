package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.repo.PayoutsRepository
import kotlinx.coroutines.launch

class PayoutsViewModel : ViewModel() {
    private val repository = PayoutsRepository()
    private val _uiState = MutableLiveData<PayoutsUiState>()
    val uiState: LiveData<PayoutsUiState> = _uiState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private var _payoutsData = MutableLiveData<List<PaymentRequestData>>()
    private var payoutsLiveData: LiveData<List<PaymentRequestData>> = _payoutsData

    init {
        loadPayouts()
    }

    fun loadPayouts() {
        _uiState.value = PayoutsUiState.Loading
        viewModelScope.launch {
            _payoutsData.value = repository.getPayoutsBySchedulerId()
            Log.e("TAG", "loadPayouts viewmodel: ${_payoutsData.value}")
        }
        payoutsLiveData.observeForever { payouts ->
            when {
                payouts.isEmpty() -> _uiState.value = PayoutsUiState.Empty
                else -> _uiState.value = PayoutsUiState.Success(payouts)
            }
        }
    }

    fun searchPayouts(query: String) {
        /*_searchQuery.value = query
        _uiState.value = PayoutsUiState.Loading

        if (query.isBlank()) {
            loadPayouts()
            return
        }

        payoutsLiveData.removeObserver { }
        //payoutsLiveData = repository.searchPayouts(query)
        payoutsLiveData.observeForever { payouts ->
            when {
                payouts.isEmpty() -> _uiState.value = PayoutsUiState.Empty
                else -> _uiState.value = PayoutsUiState.Success(payouts)
            }
        }*/
    }

    fun acceptPayout(payoutId: String,gameId: String,): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val success = repository.acceptPayout(payoutId,gameId)
                Log.d("PayoutVM", "Payout approval result: $success")
                result.value = success
            } catch (e: Exception) {
                Log.e("PayoutVM", "Error in payout approval", e)
                result.value = false
            }
        }

        return result
    }
    fun rejectPayout(payoutId: String, gameId: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            val success = repository.rejectPayout(payoutId, gameId)
            result.value = success
        }

        return result
    }
    override fun onCleared() {
        super.onCleared()
        payoutsLiveData.removeObserver { }
    }
}


// 1. UI State Sealed Class
sealed class PayoutsUiState {
    object Loading : PayoutsUiState()
    object Empty : PayoutsUiState()
    data class Success(val payouts: List<PaymentRequestData>) : PayoutsUiState()
    data class Error(val message: String) : PayoutsUiState()
}