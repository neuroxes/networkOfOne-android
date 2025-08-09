package com.example.networkofone.mvvm.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.repo.GameRepositoryImpl
import kotlinx.coroutines.launch

class GameDetailActivityViewModel : ViewModel() {

    private val repository = GameRepositoryImpl()

    private var _payoutsData = MutableLiveData<GameData>()
    var payoutsLiveData: LiveData<GameData> = _payoutsData

    fun getData(id: String) {
        viewModelScope.launch {
            _payoutsData.value = repository.getGameById(id)
        }
    }

}
