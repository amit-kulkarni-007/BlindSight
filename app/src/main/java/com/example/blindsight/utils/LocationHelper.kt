package com.example.blindsight.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

object LocationHelper {
    fun sendLocationToContacts(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationHelper", "Location permission not granted")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val locationUrl = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                val message = "Battery below 5%. My location: $locationUrl"
                SmsHelper.sendSmsToContacts(context, message)
            }
        }
    }
}
