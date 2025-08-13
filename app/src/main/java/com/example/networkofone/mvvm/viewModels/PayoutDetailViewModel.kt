package com.example.networkofone.mvvm.viewModels


    import androidx.lifecycle.LiveData
    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.networkofone.mvvm.models.GameData
    import com.example.networkofone.mvvm.models.PaymentRequestData
    import com.example.networkofone.mvvm.models.PaymentStatus
    import com.example.networkofone.mvvm.repo.GameRepositoryImpl
    import com.example.networkofone.mvvm.repo.PayoutsRepository
    import com.google.gson.Gson
    import kotlinx.coroutines.launch


    sealed class PayoutDetailUiState {
        object Loading : PayoutDetailUiState()
        data class Success(
            val gameData: GameData,
            val paymentData: PaymentRequestData,
        ) : PayoutDetailUiState()

        data class Error(val message: String) : PayoutDetailUiState()
        data class OperationSuccess(
            val message: String,
            val paymentId: String,
            val isSchool: Boolean,
        ) : PayoutDetailUiState()
    }

    class PayoutDetailViewModel : ViewModel() {
        private val repository = PayoutsRepository()
        private val gameRepository = GameRepositoryImpl()

        private val _uiState = MutableLiveData<PayoutDetailUiState>()
        val uiState: LiveData<PayoutDetailUiState> = _uiState

        private var currentPayment: PaymentRequestData? = null
        private var currentGame: GameData? = null
        private var isSchool = false

        fun setPayoutDataFromJson(json: String) {
            try {
                viewModelScope.launch {
                    currentPayment = Gson().fromJson(json, PaymentRequestData::class.java)
                    currentPayment?.gameId?.let { loadGameData(it) }
                }
            } catch (e: Exception) {
                _uiState.postValue(PayoutDetailUiState.Error("Invalid payment data"))
            }
        }

        fun loadPayoutData(id: String, isSchool: Boolean) {
            this.isSchool = isSchool
            _uiState.value = PayoutDetailUiState.Loading

            viewModelScope.launch {
                try {
                    currentPayment = if (isSchool) {
                        repository.getPayoutBySchedulerId(id)
                    } else {
                        repository.getPayoutByRefereeId(id)
                    }
                    currentPayment?.gameId?.let { loadGameData(it) }
                } catch (e: Exception) {
                    _uiState.postValue(PayoutDetailUiState.Error("Failed to load payment data"))
                }
            }
        }

        private suspend fun loadGameData(gameId: String) {
            try {
                currentGame = gameRepository.getGameById(gameId)
                currentPayment?.let { payment ->
                    currentGame?.let { game ->
                        _uiState.postValue(PayoutDetailUiState.Success(game, payment))
                    }
                }
            } catch (e: Exception) {
                _uiState.postValue(PayoutDetailUiState.Error("Failed to load game data"))
            }
        }

        // In ViewModel
        fun acceptPayout() {
            _uiState.value = PayoutDetailUiState.Loading
            viewModelScope.launch {
                try {
                    currentPayment?.let { payment ->
                        val updatedPayment = payment.copy(status = PaymentStatus.APPROVED)
                        val success = repository.acceptPayout(updatedPayment)

                        if (success) {
                            currentPayment = updatedPayment
                            // ✅ Emit Success state with updated data instead of OperationSuccess
                            currentGame?.let { game ->
                                _uiState.postValue(PayoutDetailUiState.Success(game, updatedPayment))
                            }
                            // Show success message separately if needed
                        } else {
                            _uiState.postValue(PayoutDetailUiState.Error("Failed to approve payment"))
                        }
                    }
                } catch (e: Exception) {
                    _uiState.postValue(PayoutDetailUiState.Error("Something went wrong"))
                }
            }
        }

        fun rejectPayout() {
            _uiState.value = PayoutDetailUiState.Loading
            viewModelScope.launch {
                try {
                    currentPayment?.let { payment ->
                        val updatedPayment = payment.copy(status = PaymentStatus.REJECTED)
                        val success = repository.rejectPayout(updatedPayment)

                        if (success) {
                            currentPayment = updatedPayment
                            _uiState.postValue(
                                PayoutDetailUiState.OperationSuccess(
                                    "Payment rejected", payment.id, isSchool
                                )
                            )
                        } else {
                            _uiState.postValue(PayoutDetailUiState.Error("Failed to reject payment"))
                        }
                    }
                } catch (e: Exception) {
                    _uiState.postValue(PayoutDetailUiState.Error("Something went wrong"))
                }
            }
        }

        val gameData: GameData?
            get() = currentGame
    }