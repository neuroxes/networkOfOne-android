package com.example.networkofone.customClasses

// AddressResolver.kt
import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Data class containing complete address information
 */
data class CompleteAddress(
    val streetNumber: String = "",
    val streetName: String = "",
    val subLocality: String = "",
    val locality: String = "", // City
    val subAdminArea: String = "", // County
    val adminArea: String = "", // State/Province
    val countryName: String = "",
    val countryCode: String = "",
    val postalCode: String = "",
    val featureName: String = "",
    val fullAddress: String = "",
    val premises: String = "" // Building name/number
) {
    /**
     * Returns a formatted complete address string
     */
    fun getFormattedAddress(): String {
        val addressParts = mutableListOf<String>()

        // Street address
        val streetAddress = buildString {
            if (streetNumber.isNotBlank()) append("$streetNumber ")
            if (streetName.isNotBlank()) append(streetName)
        }.trim()

        if (streetAddress.isNotBlank()) addressParts.add(streetAddress)
        if (subLocality.isNotBlank()) addressParts.add(subLocality)
        if (locality.isNotBlank()) addressParts.add(locality)
        if (subAdminArea.isNotBlank() && subAdminArea != locality) addressParts.add(subAdminArea)
        if (adminArea.isNotBlank()) addressParts.add(adminArea)
        if (postalCode.isNotBlank()) addressParts.add(postalCode)
        if (countryName.isNotBlank()) addressParts.add(countryName)

        return addressParts.joinToString(", ")
    }

    /**
     * Returns a short address (City, State, Country)
     */
    fun getShortAddress(): String {
        val parts = mutableListOf<String>()
        if (locality.isNotBlank()) parts.add(locality)
        if (adminArea.isNotBlank()) parts.add(adminArea)
        if (countryName.isNotBlank()) parts.add(countryName)
        return parts.joinToString(", ")
    }
}

/**
 * Sealed class for handling address resolution results
 */
sealed class AddressResult {
    data class Success(val address: Address?) : AddressResult()
    data class Error(val message: String) : AddressResult()
}

/**
 * AddressResolver - Converts latitude and longitude to complete address information
 */
class AddressResolver(private val context: Context) {

    private val geocoder = Geocoder(context, Locale.getDefault())

    /**
     * Get simple address string from coordinates
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Simple address string or error message
     */
    suspend fun getAddress(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext "Address service unavailable"
                }

                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses.isNullOrEmpty()) {
                    return@withContext "Address not found"
                }

                // Return the first address line (complete formatted address)
                addresses[0].getAddressLine(0) ?: "Address not available"

            } catch (e: Exception) {
                "Unable to get address"
            }
        }
    }
}
