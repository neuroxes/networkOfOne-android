package com.example.networkofone.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.networkofone.R
import com.example.networkofone.databinding.ActivityGameDetailBinding
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.UserModel
import com.example.networkofone.utils.NumberFormatterUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameDetailBinding
    private lateinit var gameData: GameData
    private var userData: UserModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupViews()
        onClicks()
    }

    private fun onClicks() {
        binding.apply {
            ivBack.setOnClickListener { finish() }
            // Return button
            btnReqPay.setOnClickListener {

            }

            // Check In button
            checkInBtn.setOnClickListener {
                handleCheckIn()
            }

            // Directions button
            directionsIcon.setOnClickListener {
                openDirections()
            }

        }
    }

    private fun getIntentData() {
        val gameDataJson = intent.getStringExtra("game_data")
        if (gameDataJson != null) {
            gameData = Gson().fromJson(gameDataJson, GameData::class.java)
        } else {
            gameData = GameData() // Initialize with default if no data is passed
        }

        userData = SharedPrefManager(this).getUser()
    }

    private fun setupViews() {
        bindGameData()
        updateStatusCard()
        updateActionButtons()
    }

    @SuppressLint("SetTextI18n")
    private fun bindGameData() {
        // Basic game info
        binding.apply {
            gameTitle.text = gameData.title
            specialNote.text = gameData.specialNote
            feeAmountText.text = "$${NumberFormatterUtil.format(gameData.feeAmount)}"
            locationText.text = gameData.location

            // Date and time
            dateText.text = formatDate(gameData.date)
            timeText.text = gameData.time

            // Team information
            createdByText.text = gameData.createdBySchoolId
            refereeText.text = userData?.name

            // Timestamps
            //createdAtText.text = formatTimestamp(gameData.createdAt)

            /*gameData.acceptedAt?.let { acceptedTime ->
                updatedAtText.text = formatTimestamp(acceptedTime)
            } ?: run {
                updatedAtText.text = formatTimestamp(gameData.createdAt)
            }*/
        }

    }

    private fun updateStatusCard() {
        binding.apply {
            when (gameData.status) {
                GameStatus.PENDING -> {
                    statusCard.setCardBackgroundColor(
                        ContextCompat.getColor(
                            this@GameDetailActivity, R.color.status_pending
                        )
                    )
                    statusIcon.setImageResource(R.drawable.ic_clock)
                    statusText.text = "Pending"
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
            when (gameData.status) {
                GameStatus.ACCEPTED -> {
                    btnAccept.isEnabled = false
                    if (gameData.checkInStatus) {
                        checkInBtn.text = "Checked In"
                        checkInBtn.isEnabled = false
                        btnReqPay.isEnabled = true
                    } else {
                        checkInBtn.text = "Check In"
                        checkInBtn.isEnabled = true
                        btnReqPay.isEnabled = false
                    }
                }

                GameStatus.CHECKED_IN -> {
                    checkInBtn.text = "Checked In"
                    checkInBtn.isEnabled = false
                    btnReqPay.isEnabled = true
                    btnAccept.isEnabled = false
                }

                GameStatus.COMPLETED -> {
                    checkInBtn.text = "Completed"
                    checkInBtn.isEnabled = false
                    btnReqPay.isEnabled = false
                    btnAccept.isEnabled = false
                }

                GameStatus.PENDING -> {
                    btnAccept.isEnabled = true
                    checkInBtn.isEnabled = false
                    btnReqPay.isEnabled = false
                }

                GameStatus.REJECTED -> {
                    btnAccept.isEnabled = false
                    checkInBtn.isEnabled = false
                    btnReqPay.isEnabled = false
                }
            }

        }
    }


    private fun handleCheckIn() {
        if (gameData.status == GameStatus.ACCEPTED && !gameData.checkInStatus) {
            // Update the game data
            gameData.checkInStatus = true
            gameData.checkInTime = System.currentTimeMillis()
            gameData.status = GameStatus.CHECKED_IN

            // Update UI
            updateStatusCard()
            updateActionButtons()

            // Here you would typically update the data in your database/API
            // updateGameInDatabase(gameData)

            // Show success message
            // Snackbar.make(findViewById(android.R.id.content), "Successfully checked in!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openDirections() {
        if (gameData.latitude != 0.0 && gameData.longitude != 0.0) {
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
        } else {
            // Fallback to search by location name
            val uri = "geo:0,0?q=${Uri.encode(gameData.location)}".toUri()
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
        } catch (e: Exception) {
            dateString
        }
    }


}