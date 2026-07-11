package com.example.synapse

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import android.util.Log

private const val TAG = "SmsLocationHelper"

/**
 * Handles fetching the last known GPS location and sending an automated
 * emergency SMS with a Google Maps link to the configured contact.
 */
class SmsLocationHelper(private val context: Context) {

    @SuppressLint("MissingPermission") // Caller must ensure permissions are granted first
    fun getLastKnownLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try GPS first, fall back to network-based location if GPS has no fix yet
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    Log.d(TAG, "Got location from $provider: ${location.latitude}, ${location.longitude}")
                    return location
                }
            } catch (e: Exception) {
                Log.w(TAG, "Provider $provider unavailable: ${e.message}")
            }
        }

        Log.w(TAG, "No last known location available from any provider")
        return null
    }

    @SuppressLint("MissingPermission") // Caller must ensure SEND_SMS permission is granted first
    fun sendEmergencySms(phoneNumber: String, contactMessage: String, reason: String) {
        if (phoneNumber.isBlank()) {
            Log.w(TAG, "No phone number configured — cannot send emergency SMS")
            return
        }

        val location = getLastKnownLocation()
        val locationText = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "(location unavailable)"
        }

        val fullMessage = buildString {
            append(contactMessage.ifBlank { "SYNAPSE Alert: possible episode detected." })
            append(" Reason: $reason.")
            append(" Location: $locationText")
        }

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            // SMS has a ~160 char limit per segment; divideMessage splits automatically if longer
            val parts = smsManager.divideMessage(fullMessage)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d(TAG, "Emergency SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}", e)
        }
    }
}