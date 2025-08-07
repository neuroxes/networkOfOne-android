package com.example.networkofone

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
import com.example.networkofone.home.HomeFragment
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.repo.GameRepository
import com.example.networkofone.mvvm.viewModels.GameViewModel
import com.example.networkofone.mvvm.viewModels.GameViewModelFactory
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity(), LocationHelper.LocationResultListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragDashboard: AppFragment
    private lateinit var fragMore: AppFragment
    private lateinit var loader: LoadingDialog
    private lateinit var locationHelper: LocationHelper
    private lateinit var etLocation: EditText

    private lateinit var gameViewModel: GameViewModel
    private var selectedDate: String = ""
    private var selectedTime: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = LoadingDialog(this)
        fragDashboard = findViewById(R.id.fragDashboard)
        fragDashboard.onAppFragmentLoader = HomeFragment(this)

        fragMore = findViewById(R.id.fragMore)
        fragMore.onAppFragmentLoader = HomeFragment(this)

        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)

        loadFragment(0)

        binding.btmNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.dashboard -> loadFragment(0)
                R.id.more_tab -> loadFragment(1)
                else -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        showCreateGameDialog()
                    }, 200)
                }
            }
            it.itemId != R.id.button_create
        }

        binding.btmNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.button_create) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showCreateGameDialog()
                }, 200)

            }
        }


        setupViewModel()
    }

    private fun setupViewModel() {
        val repository = GameRepository()
        val factory = GameViewModelFactory(repository)
        gameViewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

        gameViewModel.saveGameResult.observe(this) { result ->
            result.fold(onSuccess = { gameId ->
                NewToastUtil.showSuccess(this@MainActivity, "Game created successfully!")
                loader.endLoadingAnimation()

            }, onFailure = { exception ->
                {
                    NewToastUtil.showError(
                        this@MainActivity, "Failed to create game: ${exception.message}"

                    )
                    loader.endLoadingAnimation()
                }
            })
        }
    }

    private fun showCreateGameDialog() {
        try {
            val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
                this@MainActivity, DialogCreateGameBinding::inflate
            )
            dialog.show()

            dialogBinding.apply {
                // Setup text watchers
                setupTextWatchers()

                // Setup date and time pickers
                setupDateTimePickers()

                btnCancel.setOnClickListener { dialog.dismiss() }
                ivBack.setOnClickListener { dialog.dismiss() }
                btnCurrentLoc.setOnClickListener {
                    this@MainActivity.etLocation = dialogBinding.etLocation
                    getMyCurrentLocation()
                }

                btnSave.setOnClickListener {
                    if (validateInputs(dialogBinding)) {
                        loader.startLoadingAnimation()
                        val gameData = createGameData()
                        gameViewModel.saveGame(gameData)
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
            date = etDate.text.toString().trim(),
            time = etTime.text.toString().trim(),
            feeAmount = etPrice.text.toString().trim(),
            specialNote = etDescription.text.toString().trim(),
            createdBy = userId,
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
        NewToastUtil.showError(this@MainActivity, "Error: $error")
        Log.e("Location", "Error: $error")
    }

    override fun onLocationCanceled() {
        Log.e("Location", "User canceled location request")
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        try {
            etLocation.setText("Lat: $latitude | Long: $longitude")
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

    companion object {
        private const val TAG = "MainActivity"
    }
}