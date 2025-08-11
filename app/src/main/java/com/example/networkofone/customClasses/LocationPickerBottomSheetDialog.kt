package com.example.networkofone.customClasses


// LocationPickerBottomSheetDialog.kt
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.util.*
import com.example.networkofone.R


data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

class LocationPickerBottomSheetDialog : BottomSheetDialogFragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var selectedLocation: LatLng? = null
    private var geocoder: Geocoder? = null

    private var onLocationSelectedListener: ((LocationResult) -> Unit)? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        fun newInstance(onLocationSelected: (LocationResult) -> Unit): LocationPickerBottomSheetDialog {
            return LocationPickerBottomSheetDialog().apply {
                onLocationSelectedListener = onLocationSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup full height bottom sheet
        setupFullHeightBottomSheet()

        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // Setup map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup buttons
        setupButtons(view)
    }

    private fun setupFullHeightBottomSheet() {
        dialog?.let { dialog ->
            if (dialog is BottomSheetDialog) {
                dialog.setOnShowListener {
                    val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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

        // Get current location and move camera
        getCurrentLocationAndMoveCamera()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getCurrentLocationAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                    )
                    // Set current location as initially selected
                    updateSelectedLocation(currentLatLng)
                }
            }
        }
    }

    private fun updateSelectedLocation(latLng: LatLng) {
        selectedLocation = latLng

        // Remove previous marker
        currentMarker?.remove()

        // Add new marker
        currentMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
        )

        // Move camera to selected location
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    fun getAddressFromLocation(latLng: LatLng, callback: (String) -> Unit) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                getCurrentLocationAndMoveCamera()
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
