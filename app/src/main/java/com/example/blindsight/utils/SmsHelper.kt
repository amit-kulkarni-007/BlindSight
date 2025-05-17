package com.example.blindsight.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.blindsight.managers.ContactManager

object SmsHelper {

    fun sendSmsToContacts(context: Context, message: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SmsHelper", "SMS permission not granted")
            return
        }

        val contacts = ContactManager(context).getContacts()
        if (contacts.isEmpty()) {
            Log.d("SmsHelper", "No emergency contacts to send SMS to.")
            return
        }

        val smsManager = SmsManager.getDefault()
        contacts.forEach { number ->
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                Log.d("SmsHelper", "Sent SMS to $number")
            } catch (e: Exception) {
                Log.e("SmsHelper", "Failed to send SMS to $number: ${e.message}")
            }
        }
    }
}
