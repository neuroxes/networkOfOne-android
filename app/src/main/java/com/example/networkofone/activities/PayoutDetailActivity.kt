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
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.PaymentStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.mvvm.models.UserType
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
    private var userData: UserModel? = null
    private lateinit var loader: LoadingDialog
    private val viewModel: PayoutDetailViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = LoadingDialog(this)
        userData = SharedPrefManager(this).getUser()

        getIntentData()
        observeViewModel()
        onClicks()
    }

    private fun getIntentData() {
        try {
            val payoutDataJson = intent.getStringExtra("payoutData")
            if (payoutDataJson != null) {
                viewModel.payoutData =
                    Gson().fromJson(payoutDataJson, PaymentRequestData::class.java)
                viewModel.getGameData(viewModel.payoutData.gameId)
                setupViews()
            } else {
                val payoutId = intent.getStringExtra("payoutId")
                loader.startLoadingAnimation()
                if (userData?.userType == UserType.SCHOOL) viewModel.isSchool = true
                payoutId?.let { viewModel.getData(it); }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun observeViewModel() {
        try {
            viewModel.payoutsLiveData.observe(this) {
                viewModel.payoutData = it ?: PaymentRequestData()
                viewModel.getGameData(viewModel.payoutData.gameId)
                loader.endLoadingAnimation()
                setupViews()
            }

            viewModel.updateResult.observe(this) {
                loader.endLoadingAnimation()
                if (it) {
                    NewToastUtil.showSuccess(this, "Status updated!")
                } else {
                    NewToastUtil.showError(this, "Something went wrong")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loader.endLoadingAnimation()
        }

    }

    private fun onClicks() {
        binding.apply {
            ivBack.setOnClickListener { finish() }

            btnRejectPayout.setOnClickListener {

            }
            btnAcceptPayout.setOnClickListener {
                showConfirmationDialog()
            }

            // Directions button
            directionsIcon.setOnClickListener {
                openDirections()
            }

        }
    }

    private fun showRejectConfirmationDialog(payout: PaymentRequestData) {
        AlertDialog.Builder(this).setTitle("Reject Payout")
            .setMessage("Are you sure you want to reject this payout?")
            .setPositiveButton("Reject") { _, _ ->
                loader.startLoadingAnimation()
                viewModel.payoutData.status = PaymentStatus.REJECTED
                viewModel.gameData?.status = GameStatus.REJECTED
                viewModel.rejectPayout(viewModel.payoutData)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this).setTitle("Approve Payout")
            .setMessage("Are you sure you want to approve this payout?")
            .setPositiveButton("Approve") { _, _ ->
                loader.startLoadingAnimation()
                viewModel.payoutData.status = PaymentStatus.APPROVED
                viewModel.gameData?.status = GameStatus.ACCEPTED
                viewModel.acceptPayout(viewModel.payoutData)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun setupViews() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.nestedScrollView.visible()
            updateStatusCard()
            bindPayoutDataToViews()
            setViewVisibility()
        }, 1000)

    }

    private fun setViewVisibility() {
        binding.apply {
            when (userData?.userType) {
                UserType.SCHOOL -> {
                    btnAcceptPayout.visible()
                    btnRejectPayout.visible()
                }

                UserType.REFEREE -> {
                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                }

                else -> {}
            }

        }
        Log.e(TAG, "setViewVisibility: ${userData?.userType}")
    }

    @SuppressLint("SetTextI18n")
    private fun bindPayoutDataToViews() {
        // Basic viewModel.payoutData info
        binding.apply {
            gameTitle.text = viewModel.payoutData.gameName
            specialNote.text = viewModel.gameData?.specialNote
            feeAmountText.text = "$${NumberFormatterUtil.format(viewModel.payoutData.amount)}"
            locationText.text = viewModel.gameData?.location

            // Date and time
            dateText.text = formatDate(viewModel.gameData?.date ?: "")
            timeText.text = viewModel.gameData?.time

            // Team information
            createdByText.text = viewModel.payoutData.schedularName
            if (viewModel.payoutData.refereeName.isEmpty()) {
                refereeCard.gone()
            }
            refereeText.text = viewModel.payoutData.refereeName

            // Timestamps
            createdAtText.text = formatTimestamp(viewModel.payoutData.requestedAt)

            viewModel.gameData?.checkInTime?.let { acceptedTime ->
                updatedAtText.text = formatTimestamp(acceptedTime)
            } ?: run {
                updatedAtText.text = formatTimestamp(viewModel.gameData?.checkInTime ?: 0L)
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
            when (viewModel.gameData?.status) {
                GameStatus.PENDING -> {
                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_pending
                        )
                    )
                    statusIcon.setImageResource(R.drawable.ic_clock)
                    statusText.text = "Pending"
                }

                GameStatus.PAYMENT_REQUESTED -> {
                    btnAcceptPayout.enable()
                    btnRejectPayout.enable()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_pending
                        )
                    )
                    statusIcon.setImageResource(R.drawable.round_access_alarm_24)
                    statusText.text = "Payment Requested"
                }

                GameStatus.ACCEPTED -> {
                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_confirmed
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Accepted"
                }

                GameStatus.REJECTED -> {
                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_cancelled
                        )
                    )
                    statusIcon.setImageResource(R.drawable.triangle_warning)
                    statusText.text = "Rejected"
                }

                GameStatus.CHECKED_IN -> {
                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_processing
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Checked In"
                }

                GameStatus.COMPLETED -> {

                    btnAcceptPayout.gone()
                    btnRejectPayout.gone()
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@PayoutDetailActivity, R.color.status_delivered
                        )
                    )
                    statusIcon.setImageResource(R.drawable.check_circle)
                    statusText.text = "Completed"
                }

                else -> {}
            }

        }
    }

    private fun openDirections() {
        if (viewModel.gameData?.latitude != 0.0 && viewModel.gameData?.longitude != 0.0) {
            val uri =
                "geo:${viewModel.gameData?.latitude},${viewModel.gameData?.longitude}?q=${viewModel.gameData?.latitude},${viewModel.gameData?.longitude}(${viewModel.gameData?.location})".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web browser
                val webUri =
                    "https://maps.google.com/?q=${viewModel.gameData?.latitude},${viewModel.gameData?.longitude}".toUri()
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } else {
            // Fallback to search by location name
            val uri = "geo:0,0?q=${Uri.encode(viewModel.gameData?.location)}".toUri()
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

    companion object {
        const val TAG = "PayoutDetail"
    }
}


