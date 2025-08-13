package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.repo.PayoutsRepository
import kotlinx.coroutines.launch

class PayoutsViewModel : ViewModel() {
    private val repository = PayoutsRepository()
    private val _uiState = MutableLiveData<PayoutsUiState>()
    val uiState: LiveData<PayoutsUiState> = _uiState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _currentUserType = MutableLiveData<UserType?>()

    // Combined LiveData for real-time updates and search
    private val _filteredPayouts = MediatorLiveData<List<PaymentRequestData>>()
    private var allPayoutsLiveData: LiveData<List<PaymentRequestData>>? = null

    init {
        // Initialize with empty state
        _uiState.value = PayoutsUiState.Empty

        // Set up the observer for filtered payouts
        _filteredPayouts.observeForever { filteredPayouts ->
            updateUiState(filteredPayouts)
        }
    }

    private fun updateUiState(filteredPayouts: List<PaymentRequestData>) {
        when {
            filteredPayouts.isEmpty() -> {
                val query = _searchQuery.value?.trim() ?: ""
                if (query.isNotEmpty()) {
                    _uiState.value = PayoutsUiState.Empty // No results for search
                } else {
                    _uiState.value = PayoutsUiState.Empty // No data at all
                }
            }

            else -> _uiState.value = PayoutsUiState.Success(filteredPayouts)
        }
    }

    fun loadPayouts(userType: UserType? = UserType.SCHOOL) {
        try {
            if (_currentUserType.value == userType && allPayoutsLiveData != null) {
                // Already listening for this user type, no need to reload
                return
            }

            _currentUserType.value = userType
            _uiState.value = PayoutsUiState.Loading

            // Remove all previous sources
            allPayoutsLiveData?.let { _filteredPayouts.removeSource(it) }
            _filteredPayouts.removeSource(_searchQuery)

            viewModelScope.launch {
                userType?.let {
                    // Set up real-time listener based on user type
                    allPayoutsLiveData = when (it) {
                        UserType.SCHOOL -> repository.getPayoutsBySchedulerIdLiveData()
                        UserType.REFEREE -> repository.getPayoutsByRefereeIdLiveData()
                        else -> null
                    }

                    // Observe the real-time data
                    allPayoutsLiveData?.let { liveData ->
                        _filteredPayouts.addSource(liveData) { payouts ->
                            Log.d("PayoutsVM", "Real-time data received: ${payouts.size} payouts")
                            applySearchFilter(payouts, _searchQuery.value ?: "")
                        }

                        // Also observe search query changes
                        _filteredPayouts.addSource(_searchQuery) { query ->
                            val currentPayouts = liveData.value ?: emptyList()
                            applySearchFilter(currentPayouts, query)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PayoutsVM", "Error loading payouts", e)
            _uiState.value = PayoutsUiState.Error(e.message ?: "Failed to load payouts")
        }
    }

    private fun applySearchFilter(payouts: List<PaymentRequestData>, query: String) {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            // No search query, show all payouts
            _filteredPayouts.value = payouts
            return
        }

        // Filter payouts based on search query
        val filteredPayouts = payouts.filter { payout ->
            payout.id.contains(trimmedQuery, ignoreCase = true) ||
                    payout.gameId.contains(trimmedQuery, ignoreCase = true) ||
                    payout.refereeName.contains(trimmedQuery, ignoreCase = true) ||
                    payout.schedularName.contains(trimmedQuery, ignoreCase = true) ||
                    payout.amount.contains(trimmedQuery, ignoreCase = true)
        }

        _filteredPayouts.value = filteredPayouts
    }

    fun searchPayouts(query: String) {
        _searchQuery.value = query
        // The search will be automatically applied through the observer
    }

    fun refreshData() {
        val currentUserType = _currentUserType.value
        _currentUserType.value = null // Clear current type to force reload
        loadPayouts(currentUserType) // Reload
    }

    fun acceptPayout(payout: PaymentRequestData): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val success = repository.acceptPayout(payout)
                Log.d("PayoutVM", "Payout approval result: $success")
                result.value = success
            } catch (e: Exception) {
                Log.e("PayoutVM", "Error in payout approval", e)
                result.value = false
            }
        }

        return result
    }

    fun rejectPayout(payout: PaymentRequestData): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val success = repository.rejectPayout(payout)
                Log.d("PayoutVM", "Payout rejection result: $success")
                result.value = success
            } catch (e: Exception) {
                Log.e("PayoutVM", "Error in payout rejection", e)
                result.value = false
            }
        }

        return result
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listeners
        repository.removePayoutsListeners()
        allPayoutsLiveData?.let { _filteredPayouts.removeSource(it) }
        _filteredPayouts.removeSource(_searchQuery)
    }
}

// UI State Sealed Class (unchanged)
sealed class PayoutsUiState {
    object Loading : PayoutsUiState()
    object Empty : PayoutsUiState()
    data class Success(val payouts: List<PaymentRequestData>) : PayoutsUiState()
    data class Error(val message: String) : PayoutsUiState()
}