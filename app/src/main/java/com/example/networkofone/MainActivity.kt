package com.example.networkofone

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.networkofone.databinding.ActivityMainBinding
import com.example.networkofone.databinding.DialogCreateGameBinding
import com.example.networkofone.home.HomeFragment
import com.example.networkofone.mvvm.repo.GameRepository
import com.example.networkofone.mvvm.viewModels.GameViewModel
import com.example.networkofone.mvvm.viewModels.GameViewModelFactory
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.NewToastUtil
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.incity.incity_stores.AppFragment
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.google.android.material.textfield.TextInputLayout


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragDashboard: AppFragment
    private lateinit var fragMore: AppFragment


    private lateinit var gameViewModel: GameViewModel
    private var selectedDate: String = ""
    private var selectedTime: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragDashboard = findViewById(R.id.fragDashboard)
        fragDashboard.onAppFragmentLoader = HomeFragment(this)

        fragMore = findViewById(R.id.fragMore)
        fragMore.onAppFragmentLoader = HomeFragment(this)

        loadFragment(0)

        binding.btmNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.dashboard -> loadFragment(0)
                R.id.more_tab -> loadFragment(1)
                else -> showCreateGameDialog()
            }
            it.itemId != R.id.button_create
        }

        binding.btmNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.button_create) showCreateGameDialog()
        }

        setupViewModel()
    }

    private fun setupViewModel() {
        val repository = GameRepository()
        val factory = GameViewModelFactory(repository)
        gameViewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

        gameViewModel.saveGameResult.observe(this) { result ->
            result.fold(
                onSuccess = { gameId ->
                    NewToastUtil.showSuccess(this@MainActivity,"Game created successfully!")
                    // Handle success
                },
                onFailure = { exception ->
                    NewToastUtil.showError(this@MainActivity, "Failed to create game: ${exception.message}")
                }
            )
        }
    }

    private fun showCreateGameDialog() {
        val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
            this@MainActivity,
            DialogCreateGameBinding::inflate
        )

        dialog.show()

        dialogBinding.apply {
            // Setup text watchers
            setupTextWatchers()

            // Setup date and time pickers
            setupDateTimePickers()

            btnCancel.setOnClickListener { dialog.dismiss() }
            ivBack.setOnClickListener { dialog.dismiss() }

            btnSave.setOnClickListener {
                if (validateInputs()) {
                    val gameData = createGameData()
                    gameViewModel.saveGame(gameData)
                    dialog.dismiss()
                }
            }
        }
    }


    private fun DialogCreateGameBinding.setupTextWatchers() {
        etGameName.addTextWatcher(layGame)
        etLocation.addTextWatcher(layLocation)
        etDate.addTextWatcher(layDate)
        etTime.addTextWatcher(layTime)
        etPrice.addTextWatcher(etLayPrice)

    }

    private fun DialogCreateGameBinding.setupDateTimePickers() {
        // Date picker
        etDate.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                etDate.setText(date)
            }
        }

        // Time picker
        etTime.setOnClickListener {
            showTimePicker { time ->
                selectedTime = time
                etTime.setText(time)
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(today)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(today))
                    .build()
            )
            .build()

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

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText("Select Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute

            // Check if selected date is today and time is in the past
            if (isSelectedDateToday() && isTimeInPast(selectedHour, selectedMinute)) {
                Toast.makeText(this, "Please select a future time for today's date", Toast.LENGTH_SHORT).show()
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

    private fun DialogCreateGameBinding.validateInputs(): Boolean {
        var isValid = true

        // Validate title
        if (etGameName.text.toString().trim().isEmpty()) {
            layGame.error = "Title is required"
            isValid = false
        }

        // Validate location
        if (etLocation.text.toString().trim().isEmpty()) {
            layLocation.error = "Location is required"
            isValid = false
        }

        // Validate date
        if (etDate.text.toString().trim().isEmpty()) {
            layDate.error = "Date is required"
            isValid = false
        }

        // Validate time
        if (etTime.text.toString().trim().isEmpty()) {
            layTime.error = "Time is required"
            isValid = false
        }

        // Validate fee amount
        if (etPrice.text.toString().trim().isEmpty()) {
            etLayPrice.error = "Fee amount is required"
            isValid = false
        }

        return isValid
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
                    textInputLayout.isErrorEnabled = false
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
}