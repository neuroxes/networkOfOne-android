package com.example.networkofone.activities

import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.networkofone.R
import com.example.networkofone.databinding.ActivityMainRefreeBinding
import com.example.networkofone.fcm.FCMTokenManager
import com.example.networkofone.home.RefereeHomeFragment
import com.example.networkofone.home.PayoutFragment
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.utils.LocationHelper
import com.example.networkofone.utils.NewToastUtil
import com.incity.incity_stores.AppFragment
import java.io.IOException
import java.util.Locale

class RefereeMainActivity : AppCompatActivity(), LocationHelper.LocationResultListener {
    private lateinit var binding: ActivityMainRefreeBinding
    private lateinit var fragDashboard: AppFragment
    private lateinit var fragMore: AppFragment
    private lateinit var locationHelper: LocationHelper
    private lateinit var refereeHomeFragment: RefereeHomeFragment
    private lateinit var payoutFragment: PayoutFragment
    private var lat = 0.0
    private var long = 0.0

    private val fcmTokenManager = FCMTokenManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainRefreeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fragDashboard = findViewById(R.id.fragDashboard)
        refereeHomeFragment = RefereeHomeFragment(this) { lat, long ->
            this.lat = lat
            this.long = long
            getMyCurrentLocation()
        }
        fragDashboard.onAppFragmentLoader = refereeHomeFragment

        payoutFragment = PayoutFragment(this)
        fragMore = findViewById(R.id.fragMore)
        fragMore.onAppFragmentLoader = payoutFragment

        locationHelper = LocationHelper()
        locationHelper.initialize(this, this)

        loadFragment(0)

        fcmTokenManager.initializeFCMToken(UserType.REFEREE)

        binding.btmNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.dashboard -> loadFragment(0)
                R.id.more_tab -> loadFragment(1)
            }
            true
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            when (binding.btmNav.selectedItemId) {
                R.id.dashboard -> {
                    refereeHomeFragment.refreshData()
                }

                R.id.more_tab -> {
                    payoutFragment.refreshData()
                }
            }

            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1500)
        }
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
        Log.e("Location", "Current location: $latitude, $longitude")
        val distanceInMeters = calculateDistance(
            latitude, longitude,
            lat, long
        )
        Log.e(TAG, "onLocationReceived: $distanceInMeters")
        refereeHomeFragment.onCheckInAttempt(distanceInMeters)

    }

    override fun onLocationError(error: String) {
        NewToastUtil.showError(this@RefereeMainActivity, "Error: $error")
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
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        refereeHomeFragment.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }

    companion object {
        private const val TAG = "RefereeMainActivity"
    }
}
