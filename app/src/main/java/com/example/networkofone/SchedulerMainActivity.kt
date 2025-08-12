package com.example.networkofone

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.networkofone.adapters.LocationAdapter
import com.example.networkofone.customClasses.AddressResolver
import com.example.networkofone.customClasses.LocationPickerBottomSheetDialog
import com.example.networkofone.databinding.ActivityMainBinding
import com.example.networkofone.databinding.DialogCreateGameBinding
import com.example.networkofone.databinding.DialogSearchLocationBinding
import com.example.networkofone.fcm.FCMTokenManager
import com.example.networkofone.home.PayoutFragmentScheduler
import com.example.networkofone.home.SchedulerHomeFragment
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.LocationModel
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.mvvm.repo.GameRepository
import com.example.networkofone.mvvm.viewModels.GameViewModelFactory
import com.example.networkofone.mvvm.viewModels.MainActivityViewModel
import com.example.networkofone.utils.DialogUtil
import com.example.networkofone.utils.LoadingDialog
import com.example.networkofone.utils.LocationHelper
import com.example.networkofone.utils.NewToastUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.incity.incity_stores.AppFragment
import com.incity.incity_stores.utils.KeyboardUtils
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SchedulerMainActivity : AppCompatActivity(), LocationHelper.LocationResultListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragDashboard: AppFragment
    private lateinit var fragMore: AppFragment
    private lateinit var loader: LoadingDialog
    private lateinit var locationHelper: LocationHelper
    private lateinit var tvLocation: TextView
    private lateinit var etLati: EditText
    private lateinit var etLongi: EditText

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var schedulerHomeFragment: SchedulerHomeFragment
    private lateinit var payoutFragmentScheduler: PayoutFragmentScheduler
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private var isEditing = false

    private lateinit var placesClient : PlacesClient
    private lateinit var searchLocationAdapter : LocationAdapter
    private lateinit var locationList  : MutableList<LocationModel>
    private val fcmTokenManager = FCMTokenManager()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loader = LoadingDialog(this)
        fragDashboard = findViewById(R.id.fragDashboard)
        schedulerHomeFragment = SchedulerHomeFragment(this) { gameData ->
            isEditing = true
            showCreateGameDialog(gameData)
        }
        fragDashboard.onAppFragmentLoader = schedulerHomeFragment

        payoutFragmentScheduler = PayoutFragmentScheduler(this)
        fragMore = findViewById(R.id.fragMore)
        fragMore.onAppFragmentLoader = payoutFragmentScheduler

        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)

        loadFragment(0)
        fcmTokenManager.initializeFCMToken(UserType.SCHOOL)


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
                    schedulerHomeFragment.refreshData()
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
                    this@SchedulerMainActivity, "Game updated successfully!"
                )
                else NewToastUtil.showSuccess(
                    this@SchedulerMainActivity, "Game created successfully!"
                )
                loader.endLoadingAnimation()

            }, onFailure = { exception ->
                {
                    NewToastUtil.showError(
                        this@SchedulerMainActivity, "Failed to create game: ${exception.message}"

                    )
                    loader.endLoadingAnimation()
                }
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCreateGameDialog(gameDataForEditing: GameData? = null) {
        try {
            val (dialog, dialogBinding) = DialogUtil.createBottomDialogWithBinding(
                this@SchedulerMainActivity, DialogCreateGameBinding::inflate
            )
            dialog.show()

            dialogBinding.apply {
                this@SchedulerMainActivity.tvLocation = dialogBinding.tvSearchLocation
                this@SchedulerMainActivity.etLati = dialogBinding.etLati
                this@SchedulerMainActivity.etLongi = dialogBinding.etLongi
                gameDataForEditing?.let {
                    tvHeading.text = "Edit Game"
                    btnSave.text = "Update"
                    etGameName.setText(it.title)
                    tvLocation.setText(it.location)
                    etLati.setText(it.latitude.toString())
                    etLongi.setText(it.longitude.toString())
                    etDate.text = it.date
                    etTime.text = it.time
                    etPrice.setText(it.feeAmount)
                    etDescription.setText(it.specialNote)
                }
                // Setup text watchers
                setupTextWatchers()

                // Setup date and time pickers
                setupDateTimePickers()
                etDescription.setOnTouchListener { v, event ->
                    if (v.hasFocus()) {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }
                btnCancel.setOnClickListener { dialog.dismiss() }
                ivBack.setOnClickListener { dialog.dismiss() }
                tvSearchLocation.setOnClickListener {
                    showLocationSearchDialog()
                }
                btnCurrentLoc.setOnClickListener {
                    getMyCurrentLocation()
                }
                btnFromMap.setOnClickListener {
                    val locationPicker = LocationPickerBottomSheetDialog.newInstance { result ->
                        tvSearchLocation.text = result.address
                        etLati.setText(result.latitude.toString())
                        etLongi.setText(result.longitude.toString())
                    }
                    locationPicker.show(supportFragmentManager, "LocationPicker")
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
                                schedularName = gameDataForEditing.schedularName,
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

    private fun showLocationSearchDialog(){
        val (dialog,dialogBinding) = DialogUtil.createBottomDialogWithBinding(this@SchedulerMainActivity,
            DialogSearchLocationBinding::inflate)
        dialog.show()
        locationList = mutableListOf()
        searchLocationAdapter = LocationAdapter(locationList) { location ->
            tvLocation.text = location.address

            // Fetch place details to get coordinates
            val placeFields = listOf(Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.builder(location.placeId, placeFields).build()

            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                if (latLng != null) {
                    location.latitude = latLng.latitude
                    location.longitude = latLng.longitude

                    etLati.setText(location.latitude.toString())
                    etLongi.setText(location.longitude.toString())
                }
            }.addOnFailureListener { exception ->
                NewToastUtil.showError(this@SchedulerMainActivity, "Place not found: ${exception.message}")
            }
            KeyboardUtils.hideKeyboard(this@SchedulerMainActivity)
            dialog.dismiss()
        }
        dialogBinding.apply {
            ivBack.setOnClickListener{ dialog.dismiss()}
            recyclerView.adapter = searchLocationAdapter
            Places.initialize(applicationContext, getString(R.string.MAP_API_KEY))
            placesClient = Places.createClient(this@SchedulerMainActivity)
            searchInput.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(
                charSequence: CharSequence?,
                i: Int,
                i1: Int,
                i2: Int,
            ) {}
                @SuppressLint("NotifyDataSetChanged")
                override fun onTextChanged(s: CharSequence, i: Int, i1: Int, i2: Int) {
                    if (!s.toString().isEmpty()) {
                        searchLocations(s.toString())
                    } else {
                        locationList.clear()
                        searchLocationAdapter.notifyDataSetChanged()
                    }
                }
                override fun afterTextChanged(editable: Editable?) {}
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun searchLocations(query: String?) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnCompleteListener({ task ->
            if (task.isSuccessful) {
                val response: FindAutocompletePredictionsResponse? = task.result
                if (response != null) {
                    locationList.clear()
                    for (prediction in response.autocompletePredictions) {
                        locationList.add(
                            LocationModel(
                                prediction.getPrimaryText(null).toString(),
                                prediction.getSecondaryText(null).toString(),
                                prediction.placeId
                            )
                        )
                    }
                    searchLocationAdapter.notifyDataSetChanged()
                }
            } else {
                NewToastUtil.showError(this@SchedulerMainActivity, "searchLocations: Error in fetching List of Locations\nError : ${task.exception?.message}")
                Log.e("TAG", "searchLocations: Error in fetching List of Locations\\nError : ${task.exception?.message}")
            }
        })
    }
    private fun DialogCreateGameBinding.setupTextWatchers() {
        etGameName.addTextWatcher(layGame)
        etLati.addTextWatcher(layLati)
        etLongi.addTextWatcher(layLongi)
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
            if (tvSearchLocation.text.toString().trim().isEmpty()) {
                tvSearchLocation.error = "Location is required"
                return false
            }
            tvSearchLocation.error = null

            if (etLati.text.toString().trim().isEmpty()) {
                layLati.error = "Required"
                return false
            }

            if (etLongi.text.toString().trim().isEmpty()) {
                layLongi.error = "Required"
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
            location = tvLocation.text.toString().trim(),
            latitude = this@SchedulerMainActivity.latitude,
            longitude = this@SchedulerMainActivity.longitude,
            date = etDate.text.toString().trim(),
            time = etTime.text.toString().trim(),
            feeAmount = etPrice.text.toString().trim(),
            specialNote = etDescription.text.toString().trim(),
            createdBySchoolId = userId,
            schedularName = SharedPrefManager(this@SchedulerMainActivity).getUser()?.name ?: "null",
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
        NewToastUtil.showError(this@SchedulerMainActivity, "Error: $error")
        Log.e("Location", "Error: $error")
    }

    override fun onLocationCanceled() {
        Log.e("Location", "User canceled location request")
    }

    private fun updateLocationUI(latitude: Double, longitude: Double) {
        try {
            val addressResolver = AddressResolver(this@SchedulerMainActivity)

            lifecycleScope.launch {
                val address = addressResolver.getAddress(latitude, longitude)
                tvLocation.text = address
            }
            this.latitude = latitude
            this.longitude = longitude
            etLati.setText(latitude.toString())
            etLongi.setText(longitude.toString())

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        schedulerHomeFragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "SchedulerMainActivity"
    }
}