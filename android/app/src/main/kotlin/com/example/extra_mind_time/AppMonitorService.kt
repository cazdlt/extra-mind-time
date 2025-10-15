package com.example.extra_mind_time

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class AppMonitorService : Service() {

    private val TAG = "AppMonitorService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "AppMonitorChannel"
    private var handler: Handler? = null
    private var monitoringRunnable: Runnable? = null
    private var lastCheckedApp: String? = null
    private var lastCheckTime: Long = 0
    private val recentlyShownApps = mutableSetOf<String>()

    companion object {
        const val ACTION_START_MONITORING = "com.example.extra_mind_time.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.extra_mind_time.STOP_MONITORING"
        private const val CHECK_INTERVAL = 2000L // Check every 2 seconds
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                Log.d(TAG, "Starting monitoring")
                startForegroundService()
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                Log.d(TAG, "Stopping monitoring")
                stopMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "App Monitoring Service",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                description = "Monitors app usage for mindful breaks"
                                setShowBadge(false)
                            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mindful Time Active")
                .setContentText("Monitoring your selected apps")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
    }

    private fun startMonitoring() {
        stopMonitoring() // Stop any existing monitoring
        lastCheckTime = System.currentTimeMillis()

        monitoringRunnable =
                object : Runnable {
                    override fun run() {
                        checkForAppLaunch()
                        handler?.postDelayed(this, CHECK_INTERVAL)
                    }
                }

        handler?.post(monitoringRunnable!!)
        Log.d(TAG, "Monitoring started")
    }

    private fun stopMonitoring() {
        monitoringRunnable?.let { handler?.removeCallbacks(it) }
        monitoringRunnable = null
        recentlyShownApps.clear()
        Log.d(TAG, "Monitoring stopped")
    }

    private fun checkForAppLaunch() {
        try {
            // Get selected apps from SharedPreferences
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val selectedAppsJson = prefs.getString("flutter.selected_apps", null)

            if (selectedAppsJson.isNullOrEmpty() || selectedAppsJson == "[]") {
                return
            }

            // Parse selected apps (simple JSON array parsing)
            val selectedApps = parseJsonArray(selectedAppsJson)

            if (selectedApps.isEmpty()) {
                return
            }

            // Get usage stats
            val usageStatsManager =
                    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val startTime = lastCheckTime - 1000 // Look back 1 second before last check

            val usageStatsList =
                    usageStatsManager.queryUsageStats(
                            UsageStatsManager.INTERVAL_BEST,
                            startTime,
                            currentTime
                    )

            lastCheckTime = currentTime

            if (usageStatsList.isNullOrEmpty()) {
                return
            }

            // Find the most recently used app
            val recentApp =
                    usageStatsList.filter { it.lastTimeUsed > 0 }.maxByOrNull { it.lastTimeUsed }

            if (recentApp != null &&
                            selectedApps.contains(recentApp.packageName) &&
                            !recentlyShownApps.contains(recentApp.packageName) &&
                            recentApp.packageName != lastCheckedApp &&
                            recentApp.packageName != packageName
            ) {

                Log.d(TAG, "Detected monitored app: ${recentApp.packageName}")

                // Mark as recently shown
                recentlyShownApps.add(recentApp.packageName)
                lastCheckedApp = recentApp.packageName

                // Show delay screen
                showDelayScreen(recentApp.packageName)

                // Clear after 10 seconds
                handler?.postDelayed(
                        {
                            recentlyShownApps.remove(recentApp.packageName)
                            if (lastCheckedApp == recentApp.packageName) {
                                lastCheckedApp = null
                            }
                        },
                        10000
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for app launch: ${e.message}", e)
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            json.trim()
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
            emptyList()
        }
    }

    private fun showDelayScreen(packageName: String) {
        try {
            val intent =
                    Intent(this, DelayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("packageName", packageName)
                    }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delay screen: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.d(TAG, "Service destroyed")
    }
}
