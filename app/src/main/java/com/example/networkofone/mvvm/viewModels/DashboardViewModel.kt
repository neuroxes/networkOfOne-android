package com.example.networkofone.mvvm.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.networkofone.mvvm.models.DashboardUiState
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentStatus
import com.example.networkofone.mvvm.repo.DashboardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    repository: DashboardRepository,
) : ViewModel() {

    private val gamesFlow = repository.observeGames()
    private val paymentsFlow = repository.observePaymentRequests()
    private val userFlow = repository.observeCurrentUser()

    // Combine all streams and compute UI state
    val uiState: StateFlow<DashboardUiState> = combine(
        gamesFlow, paymentsFlow, userFlow
    ) { games, payments, user ->
        // Games analytics
        val totalGames = games.size
        val gamesPending = games.count { it.status == GameStatus.PENDING }
        val gamesAccepted = games.count { it.status == GameStatus.ACCEPTED }
        val gamesCompleted = games.count { it.status == GameStatus.COMPLETED }
        val gamesCancelled = games.count { it.status == GameStatus.REJECTED }

        Log.e(TAG, "payments -> $payments: ")
        // Payout analytics
        val approvedPayments = payments.filter { it.status == PaymentStatus.APPROVED }
        val pendingPayments = payments.filter { it.status == PaymentStatus.PENDING }
        val rejectedPayments = payments.filter { it.status == PaymentStatus.REJECTED }
        val paidPayments = payments.filter { it.status == PaymentStatus.PAID }
        val totalPayoutsCount = payments.size

        val pendingPaymentsCount = payments.count {
            it.status == PaymentStatus.PENDING //|| it.status == PaymentStatus.APPROVED
        }

        // Parse "amount" safely (string in your model)
        val totalValue = payments.sumOf { it.amount.toDoubleOrNull() ?: 0.0  }

        /*
        * approvedPayments.sumOf {
            Log.e("Dashboard VM", it.amount)
            it.amount.toDoubleOrNull() ?: 0.0
        } + pendingPayments.sumOf {
            Log.e("Pending VM amounts", it.amount)
            it.amount.toDoubleOrNull() ?: 0.0
        }*/
        val avgAmount =
            if (approvedPayments.isNotEmpty()) totalValue / (payments.size) else 0.0

        Log.e("TAG", "Total Value: $totalValue - $avgAmount")

        DashboardUiState(
            isLoading = false,
            error = null,
            totalGames = totalGames,
            totalPayoutsCount = totalPayoutsCount,

            gamesPending = gamesPending,
            gamesAccepted = gamesAccepted,
            gamesCompleted = gamesCompleted,
            gamesCancelled = gamesCancelled,

            payoutTotalValue = totalValue,
            payoutPendingCount = pendingPaymentsCount,
            payoutCompletedCount = approvedPayments.size,
            payoutAverageAmount = avgAmount,

            // simple heuristics
            realTimeSyncOk = true, // we are receiving updates continuously
            dataIntegrityOk = true // parsing was successful; set false if you add validation errors
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    companion object{
        const val TAG = "Dashboard VM"
    }
}


class DashboardViewModelFactory(
    private val repository: DashboardRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}