package com.example.extra_mind_time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking if monitoring should start")
            
            val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val isMonitoring = prefs.getBoolean("flutter.is_monitoring", false)
            
            if (isMonitoring) {
                Log.d(TAG, "Monitoring was enabled, starting AppMonitorService")
                val serviceIntent = Intent(context, AppMonitorService::class.java).apply {
                    action = AppMonitorService.ACTION_START_MONITORING
                }
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service from boot: ${e.message}")
                }
            } else {
                Log.d(TAG, "Monitoring was not enabled, doing nothing")
            }
        }
    }
}
