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

    lateinit var gameData : GameData
    private val repository = GameRepositoryImpl()

    private var _payoutsData = MutableLiveData<GameData>()
    var payoutsLiveData: LiveData<GameData> = _payoutsData


    fun getData(id: String) {
        viewModelScope.launch {
            _payoutsData.value = repository.getGameById(id)
        }
    }


    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    fun updateGame(status: GameStatus) {
        viewModelScope.launch {
            val game = gameData.copy()
            Log.e(TAG, "updateGame: ${gameData.status}", )
            _updateResult.value = repository.updateGame(game, status)
        }
    }

    private val _paymentRequestResult = MutableLiveData<Result<String>>()
    val paymentRequestResult: LiveData<Result<String>> = _paymentRequestResult


    fun createPaymentRequest(paymentRequestData: PaymentRequestData) {
        try {
            viewModelScope.launch {
                _paymentRequestResult.value = repository.createPaymentRequest(paymentRequestData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPaymentRequest: ${e.message}")
        }
    }

}
