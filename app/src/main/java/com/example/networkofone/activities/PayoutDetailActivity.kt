package com.example.networkofone.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.networkofone.R
import com.example.networkofone.databinding.ActivityPayoutDetailBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.PaymentStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.models.asCurrency
import com.example.networkofone.mvvm.viewModels.PayoutDetailUiState
import com.example.networkofone.mvvm.viewModels.PayoutDetailViewModel
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.NumberFormatterUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PayoutDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPayoutDetailBinding
    private val viewModel: PayoutDetailViewModel by viewModels()
    private lateinit var loader: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        handleIntent()
    }

    private fun setupViews() {
        loader = LoadingDialog(this)
        binding.ivBack.setOnClickListener { finish() }
        binding.btnRejectPayout.setOnClickListener { showRejectConfirmationDialog() }
        binding.btnAcceptPayout.setOnClickListener { showConfirmationDialog() }
        binding.directionsIcon.setOnClickListener { openDirections() }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is PayoutDetailUiState.Loading -> loader.startLoadingAnimation()
                is PayoutDetailUiState.Success -> {
                    loader.endLoadingAnimation()
                    updateUi(state.gameData, state.paymentData)
                }
                is PayoutDetailUiState.Error -> {
                    loader.endLoadingAnimation()
                    showError(state.message)
                }
                is PayoutDetailUiState.OperationSuccess -> {
                    loader.endLoadingAnimation()
                    showSuccess(state.message)
                    // Refresh data after successful operation
                    state.paymentId.let { viewModel.loadPayoutData(it, isSchool = state.isSchool) }
                }
            }
        }
    }

    private fun handleIntent() {
        intent.getStringExtra("payoutData")?.let { json ->
            viewModel.setPayoutDataFromJson(json)
        } ?: run {
            intent.getStringExtra("payoutId")?.let { id ->
                val isSchool = SharedPrefManager(this).getUser()?.userType == UserType.SCHOOL
                viewModel.loadPayoutData(id, isSchool)
            }
        }
    }

    private fun updateUi(gameData: GameData, paymentData: PaymentRequestData) {
        binding.apply {
            nestedScrollView.visible()

            // Game information
            gameTitle.text = gameData.title
            specialNote.text = gameData.specialNote
            feeAmountText.text = (gameData.feeAmount.toDoubleOrNull()?.asCurrency())
            locationText.text = gameData.location
            dateText.text = formatDate(gameData.date)
            timeText.text = gameData.time
            createdByText.text = paymentData.schedularName

            // Referee information
            if (gameData.refereeName.isNullOrEmpty()) {
                refereeCard.gone()
            } else {
                refereeCard.visible()
                refereeText.text = gameData.refereeName
            }

            // Timestamps
            createdAtText.text = formatTimestamp(paymentData.requestedAt)
            updatedAtText.text = gameData.acceptedAt?.let { formatTimestamp(it) } ?: "N/A"

            // Update status UI based on both game and payment status
            updateStatusUi(gameData.status, paymentData.status)

            // Set button visibility based on user type and current status
            setButtonVisibility(
                userType = SharedPrefManager(this@PayoutDetailActivity).getUser()?.userType,
                gameStatus = gameData.status,
                paymentStatus = paymentData.status
            )
        }
    }

    private fun updateStatusUi(gameStatus: GameStatus, paymentStatus: PaymentStatus) {
        val (bgColor, iconRes, statusText) = when {
            paymentStatus == PaymentStatus.APPROVED -> Triple(R.color.status_confirmed, R.drawable.check_circle, "Payment Approved")
            paymentStatus == PaymentStatus.REJECTED -> Triple(R.color.status_cancelled, R.drawable.triangle_warning, "Payment Rejected")
            gameStatus == GameStatus.PAYMENT_REQUESTED -> Triple(R.color.status_pending, R.drawable.round_access_alarm_24, "Payment Requested")
            gameStatus == GameStatus.ACCEPTED -> Triple(R.color.status_confirmed, R.drawable.check_circle, "Game Accepted")
            gameStatus == GameStatus.REJECTED -> Triple(R.color.status_cancelled, R.drawable.triangle_warning, "Game Rejected")
            gameStatus == GameStatus.CHECKED_IN -> Triple(R.color.status_processing, R.drawable.check_circle, "Checked In")
            gameStatus == GameStatus.COMPLETED -> Triple(R.color.status_delivered, R.drawable.check_circle, "Completed")
            else -> Triple(R.color.status_pending, R.drawable.ic_clock, "Pending")
        }

        binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(this, bgColor))
        binding.statusIcon.setImageResource(iconRes)
        binding.statusText.text = statusText
    }

    private fun setButtonVisibility(userType: UserType?, gameStatus: GameStatus, paymentStatus: PaymentStatus) {
        val shouldShowButtons = userType == UserType.SCHOOL &&
                gameStatus == GameStatus.PAYMENT_REQUESTED &&
                paymentStatus == PaymentStatus.PENDING

        binding.btnAcceptPayout.setVisible(shouldShowButtons)
        binding.btnRejectPayout.setVisible(shouldShowButtons)
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Approve Payment")
            .setMessage("Are you sure you want to approve this payment request?")
            .setPositiveButton("Approve") { _, _ -> viewModel.acceptPayout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reject Payment")
            .setMessage("Are you sure you want to reject this payment request?")
            .setPositiveButton("Reject") { _, _ -> viewModel.rejectPayout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDirections() {
        viewModel.gameData?.let { game ->
            if (game.latitude != 0.0 && game.longitude != 0.0) {
                val uri = "geo:${game.latitude},${game.longitude}?q=${game.latitude},${game.longitude}(${game.location})".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, "https://maps.google.com/?q=${game.latitude},${game.longitude}".toUri()))
                }
            } else {
                val uri = "geo:0,0?q=${Uri.encode(game.location)}".toUri()
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

    private fun showError(message: String) {
        NewToastUtil.showError(this, message)
    }

    private fun showSuccess(message: String) {
        NewToastUtil.showSuccess(this, message)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (_: Exception) {
            dateString
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            val date = Date(timestamp)
            sdf.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }


    private fun View.visible() {
        this.visibility = VISIBLE
    }

    private fun View.gone() {
        this.visibility = GONE
    }



    private fun View.enable() {
        this.isEnabled = true
    }

    private fun View.disable() {
        this.isEnabled = false
    }


    fun View.setVisible(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }


    fun View.invisible() {
        visibility = View.INVISIBLE
    }

    fun View.setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

}