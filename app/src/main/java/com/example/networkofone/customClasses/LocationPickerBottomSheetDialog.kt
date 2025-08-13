package com.example.networkofone.customClasses


// LocationPickerBottomSheetDialog.kt
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.content.ContextCompat
import com.example.networkofone.R
import com.example.networkofone.utils.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

class LocationPickerBottomSheetDialog : BottomSheetDialogFragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var selectedLocation: LatLng? = null
    private var geocoder: Geocoder? = null
    private var locationHelper: LocationHelper? = null

    private var onLocationSelectedListener: ((LocationResult) -> Unit)? = null

    // Location callback for LocationHelper
    private val locationResultListener = object : LocationHelper.LocationResultListener {
        override fun onLocationReceived(latitude: Double, longitude: Double) {
            val currentLatLng = LatLng(latitude, longitude)

            // Move camera to current location and zoom in
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
            )

            // Set current location as initially selected
            updateSelectedLocation(currentLatLng)
        }

        override fun onLocationError(error: String) {
            // Handle location error - could show a toast or default to a city center
            // For now, we'll just log it and let the user manually select location
        }

        override fun onLocationCanceled() {
            // User canceled location request - they can still manually select location on map
        }
    }

    companion object {
        fun newInstance(onLocationSelected: (LocationResult) -> Unit): LocationPickerBottomSheetDialog {
            return LocationPickerBottomSheetDialog().apply {
                onLocationSelectedListener = onLocationSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_location_picker, container, false)
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup full height bottom sheet
        setupFullHeightBottomSheet()

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // Initialize LocationHelper
        locationHelper = LocationHelper()
        locationHelper?.initialize(this, locationResultListener)

        // Setup map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup buttons
        setupButtons(view)
    }*/

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment == null) {
            // Fragment not ready yet, try again after a delay
            view?.postDelayed({ setupMap() }, 100)
            return
        }
        mapFragment.getMapAsync(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup full height bottom sheet
        setupFullHeightBottomSheet()

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // Initialize LocationHelper
        locationHelper = LocationHelper()
        locationHelper?.initialize(this, locationResultListener)

        // Setup map with delay to ensure fragment is ready
        setupMap()

        // Setup buttons
        setupButtons(view)
    }

    private fun setupFullHeightBottomSheet() {
        dialog?.let { dialog ->
            if (dialog is BottomSheetDialog) {
                dialog.setOnShowListener {
                    val bottomSheet =
                        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    bottomSheet?.let { sheet ->
                        val behavior = BottomSheetBehavior.from(sheet)
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        behavior.isDraggable = false

                        // Set height to match parent (full screen)
                        val layoutParams = sheet.layoutParams
                        layoutParams.height = MATCH_PARENT
                        sheet.layoutParams = layoutParams
                    }
                }
            }
        }
    }

    private fun setupButtons(view: View) {
        val cancelButton = view.findViewById<MaterialButton>(R.id.btn_cancel)
        val doneButton = view.findViewById<MaterialButton>(R.id.btn_done)

        doneButton.apply {
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.brand)
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        doneButton.setOnClickListener {
            selectedLocation?.let { location ->
                getAddressFromLocation(location) { address ->
                    val result = LocationResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address
                    )
                    onLocationSelectedListener?.invoke(result)
                    dismiss()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        // Setup map click listener
        map.setOnMapClickListener { latLng ->
            updateSelectedLocation(latLng)
        }

        // Enable my location if permission is granted
        enableMyLocation()

        // Use LocationHelper to get current location with proper permission handling
        getCurrentLocationWithLocationHelper()
    }

    private fun enableMyLocation() {
        if (locationHelper?.isLocationPermissionGranted() == true) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                // Permission check failed, let LocationHelper handle it
            }
        }
    }

    private fun getCurrentLocationWithLocationHelper() {
        // Use LocationHelper to handle permissions and get current location
        locationHelper?.getCurrentLocation()
    }

    private fun updateSelectedLocation(latLng: LatLng) {
        selectedLocation = latLng

        // Remove previous marker
        currentMarker?.remove()

        // Add new marker
        currentMarker = googleMap?.addMarker(
            MarkerOptions().position(latLng).title("Selected Location")
        )

        // Move camera to selected location
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun getAddressFromLocation(latLng: LatLng, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addresses = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = if (addresses?.isNotEmpty() == true) {
                    addresses[0].getAddressLine(0) ?: "Unknown Address"
                } else {
                    "Address not found"
                }

                withContext(Dispatchers.Main) {
                    callback(address)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Unable to get address")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up LocationHelper resources
        locationHelper?.cleanup()
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}