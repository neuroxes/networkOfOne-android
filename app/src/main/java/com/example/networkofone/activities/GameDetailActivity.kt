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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = LoadingDialog(this)
        userData = SharedPrefManager(this).getUser()
        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)

        getIntentData()
        observeViewModel()
        onClicks()
    }

    private fun getIntentData() {
        try {
            val gameDataJson = intent.getStringExtra("game_data")
            if (gameDataJson != null) {
                viewModel.gameData = Gson().fromJson(gameDataJson, GameData::class.java)
                setupViews()
            } else {
                val gameId = intent.getStringExtra("gameId")
                loader.startLoadingAnimation()
                gameId?.let { viewModel.getData(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun observeViewModel() {
        try {
            viewModel.payoutsLiveData.observe(this) {
                viewModel.gameData = it ?: GameData()
                loader.endLoadingAnimation()
                setupViews()
            }

            viewModel.updateResult.observe(this) {
                loader.endLoadingAnimation()
                if (it.isSuccess) {
                    NewToastUtil.showSuccess(this, "Status updated!")
                } else {
                    NewToastUtil.showError(this, "Something went wrong")
                }
            }
            viewModel.paymentRequestResult.observe(this) { result ->
                loader.endLoadingAnimation()
                if (result.isSuccess) {
                    updateGameStatus(GameStatus.PAYMENT_REQUESTED)
                    NewToastUtil.showSuccess(this, "Payment request submitted successfully!")
                } else {
                    NewToastUtil.showError(
                        this,
                        "Failed to submit payment request: ${result.exceptionOrNull()?.message}"
                    )
                }
            }

        } catch (e: Exception) {
            loader.endLoadingAnimation()
            e.printStackTrace()
        }

    }

    private fun onClicks() {
        binding.apply {
            ivBack.setOnClickListener { finish() }

            btnReqPay.setOnClickListener {
                initiatePayoutRequest()
            }

            // Check In button
            btnAccept.setOnClickListener {
                if (btnAccept.text == "Accept") {
                    viewModel.gameData.refereeName = userData?.name
                    viewModel.gameData.acceptedByRefereeId = userData?.id
                    viewModel.gameData.acceptedAt = System.currentTimeMillis()
                    updateGameStatus(GameStatus.ACCEPTED)
                } else {
                    updateGameStatus(GameStatus.CHECKED_IN)
                }
            }

            btnReturn.setOnClickListener {
                updateGameStatus(GameStatus.PENDING)
            }


            // Directions button
            directionsIcon.setOnClickListener {
                openDirections()
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun initiatePayoutRequest() {
        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            this, LayoutProvidePaymentDetailBinding::inflate
        )
        dialog.show()
        dialogBinding.apply {
            tvAmount.text = "$${NumberFormatterUtil.format(viewModel.gameData.feeAmount)}"
            ivBack.setOnClickListener { dialog.dismiss() }
            btnCancel.setOnClickListener { dialog.dismiss() }
            paymentMethodRadioGroup.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.rBtn1 -> {
                        lay2.visibility = VISIBLE
                        setPaymentMethod(
                            "XRPL Address",
                            "Enter your XRPL wallet address",
                            R.drawable.round_currency_bitcoin_24
                        )
                    }

                    R.id.rBtn2 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Bank Account Details",
                            "Enter your bank account number",
                            R.drawable.bank
                        )
                    }

                    R.id.rBtn3 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Card Details", "Enter your credit card number", R.drawable.cvv_card
                        )
                    }

                    R.id.rBtn4 -> {
                        lay2.visibility = GONE
                        setPaymentMethod(
                            "Payment Details", "Enter your payment details", R.drawable.sack_dollar
                        )
                    }
                }
            }
            add1.setOnClickListener { etAccountDetail.setText(add1.text) }
            add2.setOnClickListener { etAccountDetail.setText(add2.text) }
            add3.setOnClickListener { etAccountDetail.setText(add3.text) }
            add4.setOnClickListener { etAccountDetail.setText(add4.text) }


            btnSave.setOnClickListener {
                if (isDataValid()) {
                    val paymentDetail = PaymentRequestData(
                        gameId = viewModel.gameData.id,
                        gameName = viewModel.gameData.title,
                        refereeId = userData?.id ?: "Null",
                        id = "",
                        refereeName = userData?.name ?: "Null",
                        schedularName = viewModel.gameData.schedularName,
                        schedularId = viewModel.gameData.createdBySchoolId,
                        amount = viewModel.gameData.feeAmount,
                        paymentMethod = when (paymentMethodRadioGroup.checkedRadioButtonId) {
                            R.id.rBtn1 -> PaymentMethod.XRPL
                            R.id.rBtn2 -> PaymentMethod.BANK_TRANSFER
                            R.id.rBtn3 -> PaymentMethod.PAYPAL
                            R.id.rBtn4 -> PaymentMethod.VENMO
                            else -> PaymentMethod.NONE
                        },
                    )
                    loader.startLoadingAnimation()
                    viewModel.createPaymentRequest(paymentDetail)
                    dialog.dismiss()
                }
            }
        }
    }

    fun LayoutProvidePaymentDetailBinding.isDataValid(): Boolean {
        val accountDetail = etAccountDetail.getText().toString().trim()
        if (accountDetail.isEmpty()) {
            etLayPrice.error = "Required"
            return false
        }
        etLayPrice.error = null
        return true
    }

    fun LayoutProvidePaymentDetailBinding.setPaymentMethod(
        title: String,
        hint: String,
        iconRes: Int,
    ) {
        t4.text = title
        etAccountDetail.hint = hint
        etAccountDetail.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(
                this@GameDetailActivity, iconRes
            ), null, null, null
        )
    }


    private fun setupViews() {
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

                else -> {}
            }

        }
        Log.e(TAG, "setViewVisibility: ${userData?.userType}")
    }

    @SuppressLint("SetTextI18n")
    private fun bindGameData() {
        // Basic viewModel.gameData info
        binding.apply {
            gameTitle.text = viewModel.gameData.title
            specialNote.text = viewModel.gameData.specialNote
            feeAmountText.text = "$${NumberFormatterUtil.format(viewModel.gameData.feeAmount)}"
            locationText.text = viewModel.gameData.location

            // Date and time
            dateText.text = formatDate(viewModel.gameData.date)
            timeText.text = viewModel.gameData.time

            // Team information
            createdByText.text = viewModel.gameData.schedularName
            if (viewModel.gameData.refereeName.isNullOrEmpty()) {
                refereeCard.gone()
            }
            refereeText.text = viewModel.gameData.refereeName

            // Timestamps
            createdAtText.text = formatTimestamp(viewModel.gameData.createdAt)

            viewModel.gameData.acceptedAt?.let { acceptedTime ->
                updatedAtText.text = formatTimestamp(acceptedTime)
            } ?: run {
                updatedAtText.text = formatTimestamp(viewModel.gameData.createdAt)
            }

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

    private fun updateStatusCard() {
        binding.apply {
            when (viewModel.gameData.status) {
                GameStatus.PENDING -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_pending
                        )
                    )
                    statusIcon.setImageResource(R.drawable.ic_clock)
                    statusText.text = "Pending"
                }

                GameStatus.PAYMENT_REQUESTED -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_pending
                        )
                    )
                    statusIcon.setImageResource(R.drawable.round_access_alarm_24)
                    statusText.text = "Payment Requested"
                }

                GameStatus.ACCEPTED -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_confirmed
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Accepted"
                }

                GameStatus.REJECTED -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_cancelled
                        )
                    )
                    statusIcon.setImageResource(R.drawable.triangle_warning)
                    statusText.text = "Rejected"
                }

                GameStatus.CHECKED_IN -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_processing
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Checked In"
                }

                GameStatus.COMPLETED -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_delivered
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Completed"
                }
            }

        }
    }

    private fun updateActionButtons() {
        binding.apply {
            when (viewModel.gameData.status) {
                GameStatus.PENDING -> {
                    btnAccept.text = "Accept"
                    btnAccept.enable()
                    btnReturn.disable()
                    btnReqPay.disable()
                }

                GameStatus.ACCEPTED -> {
                    btnAccept.text = "Check - in"
                    btnAccept.enable()
                    btnReturn.enable()
                    btnReqPay.disable()/*if (viewModel.gameData.checkInStatus) {
                        btnReturn.text = "Checked In"
                        btnReturn.disable()
                        btnReqPay.enable()
                    } else {
                        btnReturn.text = "Check In"
                        btnReturn.enable()
                        btnReqPay.disable()
                    }*/
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
                    btnReturn.disable()
                    btnReqPay.disable()
                    btnAccept.disable()
                }
            }
        }
    }


    private fun updateGameStatus(status: GameStatus) {
        loader.startLoadingAnimation()
        when (status) {
            GameStatus.PENDING, GameStatus.ACCEPTED, GameStatus.PAYMENT_REQUESTED -> {
                viewModel.gameData.status = status
            }

            GameStatus.CHECKED_IN -> {
                viewModel.gameData.checkInStatus = true
                viewModel.gameData.checkInTime = System.currentTimeMillis()
                viewModel.gameData.status = status
            }

            else -> {}
        }
        updateStatusCard()
        updateActionButtons()
        viewModel.updateGame(viewModel.gameData, status)
        bindGameData()

        /*if (viewModel.gameData.status == GameStatus.ACCEPTED && !viewModel.gameData.checkInStatus) {
            // Update the viewModel.gameData data
            viewModel.gameData.checkInStatus = true
            viewModel.gameData.checkInTime = System.currentTimeMillis()
            viewModel.gameData.status = GameStatus.CHECKED_IN

            // Update UI
            updateStatusCard()
            updateActionButtons()
            viewModel.updateGame(viewModel.gameData,status)

            // Here you would typically update the data in your database/API
            // updateGameInDatabase(viewModel.gameData)

            // Show success message
            // Snackbar.make(findViewById(android.R.id.content), "Successfully checked in!", Snackbar.LENGTH_SHORT).show()
        }*/
    }

    private fun openDirections() {
        if (viewModel.gameData.latitude != 0.0 && viewModel.gameData.longitude != 0.0) {
            val uri =
                "geo:${viewModel.gameData.latitude},${viewModel.gameData.longitude}?q=${viewModel.gameData.latitude},${viewModel.gameData.longitude}(${viewModel.gameData.location})".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser
                val webUri =
                    "https://maps.google.com/?q=${viewModel.gameData.latitude},${viewModel.gameData.longitude}".toUri()
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } else {
            // Fallback to search by location name
            val uri = "geo:0,0?q=${Uri.encode(viewModel.gameData.location)}".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
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
        // Check if permission is already granted (optional)
        if (locationHelper.isLocationPermissionGranted()) {
            locationHelper.getCurrentLocation()
        } else {
            // This will automatically request permission and then get location
            locationHelper.getCurrentLocation()
        }
    }

    override fun onLocationReceived(latitude: Double, longitude: Double) {
        // Use the received location coordinates
        Log.e("Location", "Current location: $latitude, $longitude")
        val distanceInMeters = calculateDistance(
            latitude, longitude, viewModel.gameData.latitude, viewModel.gameData.longitude
        )
        onCheckInAttempt(distanceInMeters)
        Log.e(TAG, "onLocationReceived: $distanceInMeters")
    }

    override fun onLocationError(error: String) {
        NewToastUtil.showError(this@GameDetailActivity, "Error: $error")
        Log.e("Location", "Error: $error")
    }

    override fun onLocationCanceled() {
        Log.e("Location", "User canceled location request")
    }

    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0] // distance in meters
    }


    fun onCheckInAttempt(isWithinRange: Float) {
        if (isWithinRange < 100) {
            updateGameStatus(GameStatus.CHECKED_IN)
        } else {
            NewToastUtil.showError(
                this,
                "You are not in the check-in range of the game location. Distance $isWithinRange"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationHelper.cleanup()
        } catch (e: Exception) {
            Log.e("TAG", "onDestroy: ${e.message}")
        }
    }


    companion object {
        const val TAG = "GameDetail"
    }
}


