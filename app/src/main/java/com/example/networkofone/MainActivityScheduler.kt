package com.example.networkofone

import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.databinding.ActivityMainBinding
import com.example.networkofone.databinding.DialogCreateGameBinding
import com.example.networkofone.home.HomeFragmentScheduler
import com.example.networkofone.home.PayoutFragmentScheduler
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.repo.GameRepository
import com.example.networkofone.mvvm.viewModels.GameViewModelFactory
import com.example.networkofone.mvvm.viewModels.MainActivityViewModel
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.LocationHelper
import com.example.networkofone.utils.NewToastUtil
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.incity.incity_stores.AppFragment
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivityScheduler : AppCompatActivity(), LocationHelper.LocationResultListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragDashboard: AppFragment
    private lateinit var fragMore: AppFragment
    private lateinit var loader: LoadingDialog
    private lateinit var locationHelper: LocationHelper
    private lateinit var etLocation: EditText

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var homeFragmentScheduler: HomeFragmentScheduler
    private lateinit var payoutFragmentScheduler: PayoutFragmentScheduler
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private var isEditing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = LoadingDialog(this)
        fragDashboard = findViewById(R.id.fragDashboard)
        homeFragmentScheduler = HomeFragmentScheduler(this) { gameData ->
            isEditing = true
            showCreateGameDialog(gameData)
        }
        fragDashboard.onAppFragmentLoader = homeFragmentScheduler

        payoutFragmentScheduler = PayoutFragmentScheduler(this)
        fragMore = findViewById(R.id.fragMore)
        fragMore.onAppFragmentLoader = payoutFragmentScheduler

        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)

        loadFragment(0)

        binding.btmNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.dashboard -> loadFragment(0)
                R.id.more_tab -> loadFragment(1)
                else -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        isEditing = false
                        showCreateGameDialog()
                    }, 200)
                }
            }
            it.itemId != R.id.button_create
        }

        binding.btmNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.button_create) {
                Handler(Looper.getMainLooper()).postDelayed({
                    isEditing = false
                    showCreateGameDialog()
                }, 200)

            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            when (binding.btmNav.selectedItemId) {
                R.id.dashboard -> {
                    homeFragmentScheduler.refreshData()
                }

                R.id.more_tab -> {
                    payoutFragmentScheduler.refreshData()
                }

                else -> {

                }
            }

            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1500)

        }

        setupViewModel()
    }

    private fun setupViewModel() {
        val repository = GameRepository()
        val factory = GameViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainActivityViewModel::class.java]

        viewModel.saveGameResult.observe(this) { result ->
            result.fold(onSuccess = { gameId ->
                if (isEditing) NewToastUtil.showSuccess(
                    this@MainActivityScheduler,
                    "Game updated successfully!"
                )
                else NewToastUtil.showSuccess(this@MainActivityScheduler, "Game created successfully!")
                loader.endLoadingAnimation()

            }, onFailure = { exception ->
                {
                    NewToastUtil.showError(
                        this@MainActivityScheduler, "Failed to create game: ${exception.message}"

                    )
                    loader.endLoadingAnimation()
                }
            })
        }
    }

    private fun showCreateGameDialog(gameDataForEditing: GameData? = null) {
        try {
            val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
                this@MainActivityScheduler, DialogCreateGameBinding::inflate
            )
            dialog.show()

            dialogBinding.apply {
                gameDataForEditing?.let {
                    tvHeading.text = "Edit Game"
                    btnSave.text = "Update"
                    etGameName.setText(it.title)
                    etLocation.setText(it.location)
                    etDate.text = it.date
                    etTime.text = it.time
                    etPrice.setText(it.feeAmount)
                    etDescription.setText(it.specialNote)
                }
                // Setup text watchers
                setupTextWatchers()

                // Setup date and time pickers
                setupDateTimePickers()

                btnCancel.setOnClickListener { dialog.dismiss() }
                ivBack.setOnClickListener { dialog.dismiss() }
                btnCurrentLoc.setOnClickListener {
                    this@MainActivityScheduler.etLocation = dialogBinding.etLocation
                    getMyCurrentLocation()
                }

                btnSave.setOnClickListener {
                    if (validateInputs(dialogBinding)) {
                        loader.startLoadingAnimation()
                        val gameData = createGameData()
                        if (gameDataForEditing != null) {
                            val updatedGameData = gameData.copy(
                                latitude = gameDataForEditing.latitude,
                                longitude = gameDataForEditing.longitude,
                                id = gameDataForEditing.id,
                                createdBySchoolId = gameDataForEditing.createdBySchoolId,
                                acceptedByRefereeId = gameDataForEditing.acceptedByRefereeId,
                                acceptedAt = gameDataForEditing.acceptedAt,
                                checkInStatus = gameDataForEditing.checkInStatus,
                                checkInTime = gameDataForEditing.checkInTime
                            )
                            viewModel.updateGame(updatedGameData)
                        } else viewModel.saveGame(gameData)
                        dialog.dismiss()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "showCreateGameDialog: ${e.message}")
        }
    }


    private fun DialogCreateGameBinding.setupTextWatchers() {
        etGameName.addTextWatcher(layGame)
        etLocation.addTextWatcher(layLocation)
        etPrice.addTextWatcher(etLayPrice)
    }

    private fun DialogCreateGameBinding.setupDateTimePickers() {
        // Date picker
        etDate.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                etDate.text = date
            }
        }

        // Time picker
        etTime.setOnClickListener {
            showTimePicker { time ->
                selectedTime = time
                etTime.text = time
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        val datePicker =
            MaterialDatePicker.Builder.datePicker().setTitleText("Select Date").setSelection(today)
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(today)).build()
                ).build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            calendar.timeInMillis = selection
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            onDateSelected(dateFormat.format(calendar.time))
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePicker =
            MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).setHour(hour)
                .setMinute(minute).setTitleText("Select Time").build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            // Check if selected date is today and time is in the past
            if (isSelectedDateToday() && isTimeInPast(selectedHour, selectedMinute)) {
                Toast.makeText(
                    this, "Please select a future time for today's date", Toast.LENGTH_SHORT
                ).show()
                return@addOnPositiveButtonClickListener
            }

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            onTimeSelected(timeFormat.format(calendar.time))
        }

        timePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun isSelectedDateToday(): Boolean {
        if (selectedDate.isEmpty()) return false

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val todayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val today = todayFormat.format(Date())

        return selectedDate == today
    }

    private fun isTimeInPast(hour: Int, minute: Int): Boolean {
        val now = Calendar.getInstance()
        val selectedTime = Calendar.getInstance()
        selectedTime.set(Calendar.HOUR_OF_DAY, hour)
        selectedTime.set(Calendar.MINUTE, minute)

        return selectedTime.timeInMillis <= now.timeInMillis
    }

    private fun validateInputs(dialogBinding: DialogCreateGameBinding): Boolean {
        dialogBinding.apply {
            // Validate title
            if (etGameName.text.toString().trim().isEmpty()) {
                layGame.error = "Title is required"
                return false
            }

            // Validate location
            if (etLocation.text.toString().trim().isEmpty()) {
                layLocation.error = "Location is required"
                return false
            }

            // Validate date
            if (etDate.text.toString().trim().isEmpty()) {
                etDate.error = "Date is required"
                return false
            }

            // Validate time
            if (etTime.text.toString().trim().isEmpty()) {
                etTime.error = "Time is required"
                return false
            }

            // Validate fee amount
            if (etPrice.text.toString().trim().isEmpty()) {
                etLayPrice.error = "Fee amount is required"
                return false
            }
        }
        return true
    }

    private fun DialogCreateGameBinding.createGameData(): GameData {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: ""

        return GameData(
            title = etGameName.text.toString().trim(),
            location = etLocation.text.toString().trim(),
            latitude = this@MainActivityScheduler.latitude,
            longitude = this@MainActivityScheduler.longitude,
            date = etDate.text.toString().trim(),
            time = etTime.text.toString().trim(),
            feeAmount = etPrice.text.toString().trim(),
            specialNote = etDescription.text.toString().trim(),
            createdBySchoolId = userId,
            status = GameStatus.PENDING
        )
    }

    fun EditText.addTextWatcher(textInputLayout: TextInputLayout) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    textInputLayout.error = null
                }
            }
        })
    }

    private fun loadFragment(fragIndex: Int) {
        fragDashboard.visible(fragIndex == 0)
        fragMore.visible(fragIndex == 1)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        val selectedFragmentId = when (binding.btmNav.selectedItemId) {
            R.id.dashboard -> 0
            R.id.more_tab -> 1
            else -> 0
        }
        outState.putInt("selectedFragmentId", selectedFragmentId)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadFragment(savedInstanceState.getInt("selectedFragmentId", 0))
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
        Log.d("Location", "Current location: $latitude, $longitude")

        updateLocationUI(latitude, longitude)
    }

    override fun onLocationError(error: String) {
        NewToastUtil.showError(this@MainActivityScheduler, "Error: $error")
        Log.e("Location", "Error: $error")
    }

    override fun onLocationCanceled() {
        Log.e("Location", "User canceled location request")
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        try {
            val loc = getAddressFromLocation(latitude, longitude)
            this.latitude = latitude
            this.longitude = longitude
            etLocation.setText(loc)
        } catch (e: Exception) {
            Log.e("TAG", "updateLocationUI: ${e.message}")
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

    fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        var result = ""

        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // Build the address string
                val sb = StringBuilder()

                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(", ")
                }

                // Alternatively, you can access specific components:
                val street = address.thoroughfare       // Street name
                val city = address.locality            // City
                val state = address.adminArea         // State/Province
                val country = address.countryName     // Country
                val postalCode = address.postalCode    // Postal code

                if (postalCode != null) result += "$postalCode, "
                if (street != null) result += "$street, "
                if (city != null) result += "$city, "
                if (state != null) result += "$state, "
                if (country != null) result += "$country, "

                // Remove the last comma and space if needed
                result = result.substring(0, result.length - 2)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result = "Unable to get address"
        }
        return result
    }

    companion object {
        private const val TAG = "MainActivityScheduler"
    }
}