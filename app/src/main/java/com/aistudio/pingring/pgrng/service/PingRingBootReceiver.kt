package com.aistudio.pingring.pgrng.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aistudio.pingring.pgrng.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Automatically starts the background listening service when the phone turns on/reboots.
 */
class PingRingBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("PingRingBootReceiver", "Boot action received: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val user = db.userDao().getCurrentUser()
                if (user != null && user.pairingCode.isNotBlank()) {
                    PingRingForegroundService.start(context)
                }
            }
        }
    }
}
