package com.example.networkofone.mvvm.viewModels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import com.example.networkofone.mvvm.repo.PayoutsRepository
import kotlinx.coroutines.launch

class PayoutDetailViewModel : ViewModel() {

    lateinit var payoutData: PaymentRequestData
    var gameData: GameData? = null

    private val repository = PayoutsRepository()

    private var _payoutsData = MutableLiveData<PaymentRequestData>()
    var payoutsLiveData: LiveData<PaymentRequestData> = _payoutsData

    var isSchool = false

    fun getData(id: String) {
        viewModelScope.launch {
            _payoutsData.value =
                if (isSchool) repository.getPayoutBySchedulerId(id) else repository.getPayoutByRefereeId(
                    id
                )
        }
    }

    fun getGameData(id: String) {
        viewModelScope.launch {
            gameData = GameRepositoryImpl().getGameById(id)
        }
    }


    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    fun acceptPayout(game: PaymentRequestData) {
        viewModelScope.launch {
            _updateResult.value = repository.acceptPayout(game)
        }
    }

    fun rejectPayout(game: PaymentRequestData) {
        viewModelScope.launch {
            _updateResult.value = repository.rejectPayout(game)
        }
    }

}
