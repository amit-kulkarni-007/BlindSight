package com.example.blindsight.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat

object SmsHelper {
    private val emergencyContacts = listOf("+911234567890")

    fun sendSmsToContacts(context: Context, message: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SmsHelper", "SMS permission not granted")
            return
        }

        val smsManager = SmsManager.getDefault()
        emergencyContacts.forEach { number ->
            smsManager.sendTextMessage(number, null, message, null, null)
            Log.d("SmsHelper", "Sent SMS to $number")
        }
    }
}
