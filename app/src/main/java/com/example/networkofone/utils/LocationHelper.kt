package com.example.networkofone.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationHelper {

    companion object {
        private const val LOCATION_REQUEST_TIMEOUT = 30000L // 30 seconds timeout
        private const val TAG = "LocationHelper"
    }

    // Interfaces
    interface LocationResultListener {
        fun onLocationReceived(latitude: Double, longitude: Double)
        fun onLocationError(error: String)
        fun onLocationCanceled()
    }

    // Private variables
    private var context: Context? = null
    private var activity: FragmentActivity? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private var progressDialog: AlertDialog? = null
    private var locationUpdateCallback: LocationResultListener? = null
    private var locationTimeoutHandler: Handler? = null
    private var callback: LocationResultListener? = null

    // Activity result launchers
    private var locationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    // Location callback for updates
    private val gmsLocationResultListener = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            lastKnownLocation = locationResult.lastLocation
            if (lastKnownLocation != null) {
                Log.d(TAG, "Received location update: $lastKnownLocation")
                handleLocationResult()
            } else {
                Log.d(TAG, "Location update received but location is null")
            }
            removeLocationUpdates()
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)
            if (!locationAvailability.isLocationAvailable) {
                Log.d(TAG, "Location services not available yet")
            }
        }
    }

    /**
     * Initialize the LocationHelper with a Fragment
     */
    fun initialize(fragment: Fragment, callback: LocationResultListener) {
        this.activity = fragment.requireActivity()
        this.context = fragment.requireContext()
        this.callback = callback
        setupActivityResultLaunchers(fragment)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    /**
     * Initialize the LocationHelper with an Activity
     */
    fun initialize(activity: AppCompatActivity, callback: LocationResultListener) {
        this.activity = activity
        this.context = activity
        this.callback = callback
        setupActivityResultLaunchers(activity)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    private fun setupActivityResultLaunchers(fragment: Fragment) {
        locationPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            locationPermissionGranted = isGranted
            if (isGranted) {
                checkLocationSettings()
            } else {
                showToast("Permission denied. Cannot get location.")
                callback?.onLocationError("Location permission denied")
            }
        }

        locationSettingsLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleLocationSettingsResult(result.resultCode)
        }
    }

    private fun setupActivityResultLaunchers(activity: AppCompatActivity) {
        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            locationPermissionGranted = isGranted
            if (isGranted) {
                checkLocationSettings()
            } else {
                showToast("Permission denied. Cannot get location.")
                callback?.onLocationError("Location permission denied")
            }
        }

        locationSettingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleLocationSettingsResult(result.resultCode)
        }
    }

    private fun handleLocationSettingsResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            // Location services were just enabled - add a small delay
            Handler(Looper.getMainLooper()).postDelayed({
                val locationRequest = createLocationRequest()
                getDeviceLocation(locationRequest)
            }, 1000) // 1 second delay to allow services to initialize
        } else {
            showToast("Location services not enabled")
            dismissProgressDialog()
            callback?.onLocationCanceled()
        }
    }

    /**
     * Start the location retrieval process
     * Call this method to get the current location
     */
    fun getCurrentLocation() {
        getLocationPermission()
    }

    /**
     * Check if location permission is already granted
     */
    fun isLocationPermissionGranted(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    private fun getLocationPermission() {
        context?.let { ctx ->
            when {
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    locationPermissionGranted = true
                    checkLocationSettings()
                }

                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } == true -> {
                    showPermissionRationale()
                }

                else -> {
                    locationPermissionLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    private fun showPermissionRationale() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission to find places near you. Please grant the permission.")
                .setPositiveButton("OK") { _, _ ->
                    locationPermissionLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    callback?.onLocationCanceled()
                }
                .create()
                .show()
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = createLocationRequest()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        activity?.let { act ->
            val result: Task<LocationSettingsResponse> =
                LocationServices.getSettingsClient(act)
                    .checkLocationSettings(builder.build())

            result.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                    getDeviceLocation(locationRequest)
                } catch (exception: ApiException) {
                    when (exception.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resolvable = exception as ResolvableApiException
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(resolvable.resolution).build()
                                locationSettingsLauncher?.launch(intentSenderRequest)
                            } catch (e: IntentSender.SendIntentException) {
                                showLocationSettingsError()
                            } catch (e: ClassCastException) {
                                showLocationSettingsError()
                            }
                        }

                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            showLocationSettingsError()
                        }
                    }
                }
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }
    }

    private fun showLocationSettingsError() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Location Services Unavailable")
                .setMessage("Please enable location services to use this feature.")
                .setPositiveButton("Settings") { dialog, _ ->
                    ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    callback?.onLocationCanceled()
                }
                .show()
        }
    }

    private fun getDeviceLocation(locationRequest: LocationRequest) {
        try {
            if (!locationPermissionGranted) {
                callback?.onLocationError("Location permission not granted")
                return
            }

            showProgressDialog()
            setupLocationTimeout()

            val locationResult = fusedLocationProviderClient?.lastLocation
            locationResult?.addOnCompleteListener { task ->
                activity?.let {
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            handleLocationResult()
                        } else {
                            Log.d(TAG, "Last location null, requesting updates")
                            requestLocationUpdates(locationRequest)
                        }
                    } else {
                        Log.d(TAG, "Location task failed, requesting updates")
                        requestLocationUpdates(locationRequest)
                    }
                }
            }
        } catch (e: SecurityException) {
            dismissProgressDialog()
            showToast("Security exception occurred")
            Log.e(TAG, "getDeviceLocation: Error : ${e.message}")
            callback?.onLocationError("Security exception: ${e.message}")
        }
    }

    private fun requestLocationUpdates(locationRequest: LocationRequest) {
        try {
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                gmsLocationResultListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            dismissProgressDialog()
            showToast("Cannot request location updates")
            Log.e(TAG, "requestLocationUpdates: Error : ${e.message}")
            callback?.onLocationError("Cannot request location updates: ${e.message}")
        }
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(gmsLocationResultListener)
        cancelLocationTimeout()
    }

    private fun handleLocationResult() {
        if (lastKnownLocation == null) {
            dismissProgressDialog()
            showToast("Unable to get location. Please try again.")
            callback?.onLocationError("Unable to get location")
            return
        }

        val latitude = lastKnownLocation!!.latitude
        val longitude = lastKnownLocation!!.longitude

        Log.d(TAG, "Location received: $latitude, $longitude")
        callback?.onLocationReceived(latitude, longitude)
        dismissProgressDialog()
    }

    private fun showProgressDialog() {
        context?.let { ctx ->
            dismissProgressDialog() // Dismiss any existing dialog

            progressDialog = AlertDialog.Builder(ctx)
                .setMessage("Getting Location...")
                .setCancelable(false)
                .create()

            progressDialog?.show()
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.let { dialog ->
            if (dialog.isShowing) {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing dialog: ${e.message}")
                }
            }
        }
        progressDialog = null
    }

    private fun setupLocationTimeout() {
        cancelLocationTimeout()
        locationTimeoutHandler = Handler(Looper.getMainLooper())
        locationTimeoutHandler?.postDelayed({
            if (lastKnownLocation == null) {
                showToast("Location request timed out")
                removeLocationUpdates()
                dismissProgressDialog()
                callback?.onLocationError("Location request timed out")
            }
        }, LOCATION_REQUEST_TIMEOUT)
    }

    private fun cancelLocationTimeout() {
        locationTimeoutHandler?.removeCallbacksAndMessages(null)
        locationTimeoutHandler = null
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clean up resources - call this in onDestroy()
     */
    fun cleanup() {
        removeLocationUpdates()
        dismissProgressDialog()
        cancelLocationTimeout()
        context = null
        activity = null
        callback = null
    }
}