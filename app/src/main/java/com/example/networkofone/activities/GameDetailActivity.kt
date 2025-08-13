package com.example.networkofone.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.networkofone.R
import com.example.networkofone.databinding.ActivityGameDetailBinding
import com.example.networkofone.databinding.LayoutProvidePaymentDetailBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentMethod
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.viewModels.GameDetailActivityViewModel
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.LocationHelper
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.NumberFormatterUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameDetailActivity : AppCompatActivity(), LocationHelper.LocationResultListener {
    private lateinit var binding: ActivityGameDetailBinding
    private var userData: UserModel? = null
    private lateinit var loader: LoadingDialog
    private val viewModel: GameDetailActivityViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper
    private var isLocationCheckForCheckIn = false // Flag to track location check purpose

    companion object {
        const val TAG = "GameDetail"
        private const val CHECK_IN_DISTANCE_METERS = 100f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        getIntentData()
        observeViewModel()
        onClicks()
    }

    private fun initializeComponents() {
        loader = LoadingDialog(this)
        userData = SharedPrefManager(this).getUser()
        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)
    }

    private fun getIntentData() {
        try {
            val gameDataJson = intent.getStringExtra("game_data")
            if (gameDataJson != null) {
                val gameData = Gson().fromJson(gameDataJson, GameData::class.java)
                viewModel.setGameData(gameData)
                setupViews()
            } else {
                val gameId = intent.getStringExtra("gameId")
                if (gameId != null) {
                    loader.startLoadingAnimation()
                    viewModel.getData(gameId)
                } else {
                    NewToastUtil.showError(this, "Invalid game data")
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting intent data: ${e.message}")
            NewToastUtil.showError(this, "Error loading game data")
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.gameDataLiveData.observe(this) { gameData ->
            loader.endLoadingAnimation()
            if (gameData != null) {
                setupViews()
            } else {
                NewToastUtil.showError(this, "Failed to load game data")
                finish()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            loader.endLoadingAnimation()
            if (result.isSuccess) {
                // Refresh game data to get latest status
                viewModel.getData(viewModel.getGameData().id)
                NewToastUtil.showSuccess(this, "Status updated!")
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Something went wrong"
                NewToastUtil.showError(this, errorMessage)
            }
        }

        viewModel.paymentRequestResult.observe(this) { result ->
            loader.endLoadingAnimation()
            if (result.isSuccess) {
                // Update local game status and refresh UI
                viewModel.updateLocalGameStatus(GameStatus.PAYMENT_REQUESTED)
                setupViews() // Refresh UI with new status
                NewToastUtil.showSuccess(this, "Payment request submitted successfully!")
            } else {
                val errorMessage =
                    result.exceptionOrNull()?.message ?: "Failed to submit payment request"
                NewToastUtil.showError(this, errorMessage)
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            if (isLoading) {
                loader.startLoadingAnimation()
            } else {
                loader.endLoadingAnimation()
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            loader.endLoadingAnimation()
            if (errorMessage.isNotEmpty()) {
                NewToastUtil.showError(this, errorMessage)
            }
        }
    }

    private fun onClicks() {
        binding.apply {
            ivBack.setOnClickListener { finish() }

            btnReqPay.setOnClickListener {
                initiatePayoutRequest()
            }

            btnAccept.setOnClickListener {
                handleAcceptButtonClick()
            }

            btnReturn.setOnClickListener {
                updateGameStatus(GameStatus.PENDING)
            }

            directionsIcon.setOnClickListener {
                openDirections()
            }
        }
    }

    private fun handleAcceptButtonClick() {
        val currentStatus = viewModel.getGameData().status
        when (currentStatus) {
            GameStatus.PENDING -> {
                updateGameStatus(GameStatus.ACCEPTED,userData?.name)
            }

            GameStatus.ACCEPTED -> {
                // Check if user is within check-in range
                isLocationCheckForCheckIn = true
                getMyCurrentLocation()
            }

            else -> {
                Log.e(TAG, "Unexpected status for accept button click: $currentStatus")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initiatePayoutRequest() {
        // Ensure user can only request payment when checked in
        if (viewModel.getGameData().status != GameStatus.CHECKED_IN) {
            NewToastUtil.showError(this, "You can only request payment after checking in")
            return
        }

        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            this, LayoutProvidePaymentDetailBinding::inflate
        )

        dialog.show()
        setupPaymentDialog(dialog, dialogBinding)
    }

    @SuppressLint("SetTextI18n")
    private fun setupPaymentDialog(
        dialog: android.app.Dialog,
        dialogBinding: LayoutProvidePaymentDetailBinding,
    ) {
        dialogBinding.apply {
            tvAmount.text = "$${NumberFormatterUtil.format(viewModel.getGameData().feeAmount)}"

            ivBack.setOnClickListener { dialog.dismiss() }
            btnCancel.setOnClickListener { dialog.dismiss() }

            setupPaymentMethodSelection(this)
            setupQuickAddButtons(this)

            btnSave.setOnClickListener {
                if (isDataValid()) {
                    val paymentDetail = createPaymentRequestData(this)
                    viewModel.createPaymentRequest(paymentDetail)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun setupPaymentMethodSelection(dialogBinding: LayoutProvidePaymentDetailBinding) {
        dialogBinding.paymentMethodRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rBtn1 -> {
                    dialogBinding.lay2.visibility = VISIBLE
                    dialogBinding.setPaymentMethod(
                        "XRPL Address",
                        "Enter your XRPL wallet address",
                        R.drawable.round_currency_bitcoin_24
                    )
                }

                R.id.rBtn2 -> {
                    dialogBinding.lay2.visibility = GONE
                    dialogBinding.setPaymentMethod(
                        "Bank Account Details", "Enter your bank account number", R.drawable.bank
                    )
                }

                R.id.rBtn3 -> {
                    dialogBinding.lay2.visibility = GONE
                    dialogBinding.setPaymentMethod(
                        "Card Details", "Enter your credit card number", R.drawable.cvv_card
                    )
                }

                R.id.rBtn4 -> {
                    dialogBinding.lay2.visibility = GONE
                    dialogBinding.setPaymentMethod(
                        "Payment Details", "Enter your payment details", R.drawable.sack_dollar
                    )
                }
            }
        }
    }

    private fun setupQuickAddButtons(dialogBinding: LayoutProvidePaymentDetailBinding) {
        dialogBinding.apply {
            add1.setOnClickListener { etAccountDetail.setText(add1.text) }
            add2.setOnClickListener { etAccountDetail.setText(add2.text) }
            add3.setOnClickListener { etAccountDetail.setText(add3.text) }
            add4.setOnClickListener { etAccountDetail.setText(add4.text) }
        }
    }

    private fun createPaymentRequestData(dialogBinding: LayoutProvidePaymentDetailBinding): PaymentRequestData {
        return PaymentRequestData(
            gameId = viewModel.getGameData().id,
            gameName = viewModel.getGameData().title,
            refereeId = userData?.id ?: "",
            id = "",
            refereeName = userData?.name ?: "",
            schedularName = viewModel.getGameData().schedularName,
            schedularId = viewModel.getGameData().createdBySchoolId,
            amount = viewModel.getGameData().feeAmount,
            paymentMethod = getSelectedPaymentMethod(dialogBinding)
        )
    }

    private fun getSelectedPaymentMethod(dialogBinding: LayoutProvidePaymentDetailBinding): PaymentMethod {
        return when (dialogBinding.paymentMethodRadioGroup.checkedRadioButtonId) {
            R.id.rBtn1 -> PaymentMethod.XRPL
            R.id.rBtn2 -> PaymentMethod.BANK_TRANSFER
            R.id.rBtn3 -> PaymentMethod.PAYPAL
            R.id.rBtn4 -> PaymentMethod.VENMO
            else -> PaymentMethod.NONE
        }
    }

    private fun LayoutProvidePaymentDetailBinding.isDataValid(): Boolean {
        val accountDetail = etAccountDetail.text?.toString()?.trim()
        if (accountDetail.isNullOrEmpty()) {
            etLayPrice.error = "Required"
            return false
        }
        etLayPrice.error = null
        return true
    }

    private fun LayoutProvidePaymentDetailBinding.setPaymentMethod(
        title: String,
        hint: String,
        iconRes: Int,
    ) {
        t4.text = title
        etAccountDetail.hint = hint
        etAccountDetail.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(this@GameDetailActivity, iconRes), null, null, null
        )
    }

    private fun setupViews() {
        if (!::binding.isInitialized) return

        binding.nestedScrollView.visible()
        setViewVisibility()
        bindGameData()
        updateStatusCard()
        updateActionButtons()
    }

    private fun setViewVisibility() {
        binding.apply {
            when (userData?.userType) {
                UserType.SCHOOL -> {
                    btnAccept.gone()
                    btnReturn.gone()
                    btnReqPay.gone()
                }

                UserType.REFEREE -> {
                    btnAccept.visible()
                    btnReturn.visible()
                    btnReqPay.visible()
                }

                else -> {
                    // Handle unknown user types gracefully
                    btnAccept.gone()
                    btnReturn.gone()
                    btnReqPay.gone()
                }
            }
        }
        Log.d(TAG, "User type: ${userData?.userType}")
    }

    @SuppressLint("SetTextI18n")
    private fun bindGameData() {
        val gameData = viewModel.getGameData()
        binding.apply {
            gameTitle.text = gameData.title
            specialNote.text = gameData.specialNote
            feeAmountText.text = "$${NumberFormatterUtil.format(gameData.feeAmount)}"
            locationText.text = gameData.location

            dateText.text = formatDate(gameData.date)
            timeText.text = gameData.time

            createdByText.text = gameData.schedularName

            if (gameData.refereeName.isNullOrEmpty()) {
                refereeCard.gone()
            } else {
                refereeCard.visible()
                refereeText.text = gameData.refereeName
            }

            createdAtText.text = formatTimestamp(gameData.createdAt)

            val displayTime = gameData.acceptedAt ?: gameData.createdAt
            updatedAtText.text = formatTimestamp(displayTime)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            if (timestamp <= 0) return "Not available"
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            val date = Date(timestamp)
            sdf.format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp: ${e.message}")
            "Invalid date"
        }
    }

    private fun updateStatusCard() {
        val gameData = viewModel.getGameData()
        binding.apply {
            when (gameData.status) {
                GameStatus.PENDING -> {
                    setStatusCard(R.color.status_pending, R.drawable.ic_clock, "Pending")
                }

                GameStatus.PAYMENT_REQUESTED -> {
                    setStatusCard(
                        R.color.status_pending,
                        R.drawable.round_access_alarm_24,
                        "Payment Requested"
                    )
                }

                GameStatus.ACCEPTED -> {
                    setStatusCard(R.color.status_confirmed, R.drawable.check_circle, "Accepted")
                }

                GameStatus.REJECTED -> {
                    setStatusCard(R.color.status_cancelled, R.drawable.triangle_warning, "Rejected")
                }

                GameStatus.CHECKED_IN -> {
                    setStatusCard(R.color.status_processing, R.drawable.check_circle, "Checked In")
                }

                GameStatus.COMPLETED -> {
                    setStatusCard(R.color.status_delivered, R.drawable.check_circle, "Completed")
                }
            }
        }
    }

    private fun setStatusCard(colorRes: Int, iconRes: Int, text: String) {
        binding.apply {
            statusCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@GameDetailActivity, colorRes
                )
            )
            statusIcon.setImageResource(iconRes)
            statusText.text = text
        }
    }

    private fun updateActionButtons() {
        val gameData = viewModel.getGameData()
        binding.apply {
            when (gameData.status) {
                GameStatus.PENDING -> {
                    btnAccept.text = "Accept"
                    btnAccept.enable()
                    btnReturn.disable()
                    btnReqPay.disable()
                }

                GameStatus.ACCEPTED -> {
                    btnAccept.text = "Check In"
                    btnAccept.enable()
                    btnReturn.enable()
                    btnReqPay.disable()
                }

                GameStatus.CHECKED_IN -> {
                    btnAccept.disable()
                    btnReturn.disable()
                    btnReqPay.enable()
                }

                GameStatus.PAYMENT_REQUESTED -> {
                    btnAccept.disable()
                    btnReturn.disable()
                    btnReqPay.disable()
                }

                GameStatus.COMPLETED, GameStatus.REJECTED -> {
                    btnAccept.disable()
                    btnReturn.disable()
                    btnReqPay.disable()
                }
            }
        }
    }

    private fun updateGameStatus(status: GameStatus,userName: String? = null) {
        viewModel.updateGame(status, userName)
    }

    private fun openDirections() {
        val gameData = viewModel.getGameData()

        if (gameData.latitude != 0.0 && gameData.longitude != 0.0) {
            openDirectionsWithCoordinates(gameData)
        } else if (gameData.location.isNotEmpty()) {
            openDirectionsWithLocationName(gameData.location)
        } else {
            NewToastUtil.showError(this, "Location information not available")
        }
    }

    private fun openDirectionsWithCoordinates(gameData: GameData) {
        try {
            val uri =
                "geo:${gameData.latitude},${gameData.longitude}?q=${gameData.latitude},${gameData.longitude}(${gameData.location})".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser
                val webUri =
                    "https://maps.google.com/?q=${gameData.latitude},${gameData.longitude}".toUri()
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening directions: ${e.message}")
            NewToastUtil.showError(this, "Unable to open directions")
        }
    }

    private fun openDirectionsWithLocationName(location: String) {
        try {
            val uri = "geo:0,0?q=${Uri.encode(location)}".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening directions with location name: ${e.message}")
            NewToastUtil.showError(this, "Unable to open directions")
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}")
            dateString
        }
    }

    // Extension functions
    private fun View.visible() {
        this.visibility = VISIBLE
    }

    private fun View.gone() {
        this.visibility = GONE
    }

    private fun View.invisible() {
        this.visibility = INVISIBLE
    }

    private fun View.enable() {
        this.isEnabled = true
    }

    private fun View.disable() {
        this.isEnabled = false
    }

    private fun getMyCurrentLocation() {
        if (locationHelper.isLocationPermissionGranted()) {
            locationHelper.getCurrentLocation()
        } else {
            locationHelper.getCurrentLocation()
        }
    }

    override fun onLocationReceived(latitude: Double, longitude: Double) {
        Log.d(TAG, "Current location: $latitude, $longitude")

        if (isLocationCheckForCheckIn) {
            val gameData = viewModel.getGameData()
            val distanceInMeters = calculateDistance(
                latitude, longitude, gameData.latitude, gameData.longitude
            )
            Log.d(TAG, "Distance to game location: $distanceInMeters meters")
            onCheckInAttempt(distanceInMeters)
            isLocationCheckForCheckIn = false
        }
    }

    override fun onLocationError(error: String) {
        Log.e(TAG, "Location error: $error")
        NewToastUtil.showError(this, "Location error: $error")
        isLocationCheckForCheckIn = false
    }

    override fun onLocationCanceled() {
        Log.d(TAG, "Location request canceled by user")
        NewToastUtil.showError(this, "Location access required for check-in")
        isLocationCheckForCheckIn = false
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    private fun onCheckInAttempt(distanceInMeters: Float) {
        if (distanceInMeters <= CHECK_IN_DISTANCE_METERS) {
            updateGameStatus(GameStatus.CHECKED_IN)
        } else {
            val distanceText = if (distanceInMeters > 1000) {
                String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000)
            } else {
                String.format(Locale.getDefault(), "%.0f meters", distanceInMeters)
            }
            NewToastUtil.showError(
                this,
                "You are not within check-in range. You are $distanceText away from the game location."
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::locationHelper.isInitialized) {
                locationHelper.cleanup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }
}