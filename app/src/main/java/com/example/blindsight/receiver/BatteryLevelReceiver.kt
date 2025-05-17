package com.example.blindsight.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.example.blindsight.utils.LocationHelper

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        Log.d("BatteryLevelReceiver", "Battery level: $level%")
        if (level in 1..5) {
            Log.d("BatteryLevelReceiver", "Battery low: $level%")
            LocationHelper.sendLocationToContacts(context)
        }
    }
}
