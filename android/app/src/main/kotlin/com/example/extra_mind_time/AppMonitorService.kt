package com.example.extra_mind_time

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

data class SessionData(
    var startTime: Long,
    var timeLimitMinutes: Int,
    var packageName: String? = null,
    var appName: String? = null
)

class AppMonitorService : Service() {

    private val TAG = "AppMonitorService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "AppMonitorChannel"
    private var handler: Handler? = null
    private var monitoringRunnable: Runnable? = null
    private var lastCheckedApp: String? = null
    private var lastCheckTime: Long = 0
    private val recentlyShownApps = mutableSetOf<String>()
    private var isDelayScreenActive = false
    private var timeLimitedSession: SessionData? = null

    companion object {
        const val ACTION_START_MONITORING = "com.example.extra_mind_time.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.extra_mind_time.STOP_MONITORING"
        private const val CHECK_INTERVAL = 2000L // Check every 2 seconds

        @Volatile
        private var instance: AppMonitorService? = null

        fun getInstance(): AppMonitorService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
        registerDelayScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startForegroundService()
                startMonitoring()
            }

            ACTION_STOP_MONITORING -> {
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
                    NotificationManager.IMPORTANCE_DEFAULT
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

        Log.d(TAG, "createNotification - timeLimitedSession: ${timeLimitedSession?.appName}")

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)

        timeLimitedSession?.let { session ->
            val elapsedMillis = System.currentTimeMillis() - session.startTime
            val elapsedSeconds = elapsedMillis / 1000
            val totalLimitSeconds = session.timeLimitMinutes * 60
            val remainingSeconds = totalLimitSeconds - elapsedSeconds

            val contentText = if (remainingSeconds > 0) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                val secondsFormatted = seconds.toString().padStart(2, '0')
                "${session.appName ?: "App"}: $minutes:$secondsFormatted remaining"
            } else {
                "${session.appName ?: "App"}: Time expiring..."
            }

            builder.setContentTitle("Time Limit Active")
                .setContentText(contentText)

            val extendIntent = Intent("com.example.extra_mind_time.EXTEND_TIME")
            val extendPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                extendIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_input_add,
                "+1 min",
                extendPendingIntent
            )
            Log.d(TAG, "Creating time limit notification: $contentText")
        } ?: run {
            builder.setContentTitle("Extra Mind Time Active")
                .setContentText("Monitoring your selected apps")
            Log.d(TAG, "No active time limit, showing monitoring notification")
        }

        return builder.build()
    }

    private fun updateNotification() {
        Log.d(TAG, "updateNotification called")
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "updateNotification completed - notification ID: $NOTIFICATION_ID")
    }

    private fun startMonitoring() {
        stopMonitoring()
        lastCheckTime = System.currentTimeMillis()

        monitoringRunnable =
            object : Runnable {
                override fun run() {
                    checkForAppLaunch()
                    handler?.postDelayed(this, CHECK_INTERVAL)
                }
            }

        handler?.post(monitoringRunnable!!)
    }

    private fun stopMonitoring() {
        Log.d(TAG, "=== stopMonitoring called ===")
        monitoringRunnable?.let { handler?.removeCallbacks(it) }
        monitoringRunnable = null
        recentlyShownApps.clear()
        isDelayScreenActive = false
        timeLimitedSession = null
        updateNotification()
        unregisterDelayScreenReceiver()
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
            val startTime = currentTime - 5000 // Look back 5 seconds to catch recent app switches

            val usageStatsList =
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    startTime,
                    currentTime
                )

            if (usageStatsList.isNullOrEmpty()) {
                return
            }

            // Find the most recently used app that was actually used in the last 3 seconds
            val threeSecondsAgo = currentTime - 3000
            val recentApp =
                usageStatsList
                    .filter { it.lastTimeUsed > threeSecondsAgo && it.packageName != packageName }
                    .maxByOrNull { it.lastTimeUsed }

            Log.d(TAG, "recentApp: ${recentApp?.packageName}, timeLimitedSession: ${timeLimitedSession?.appName}")

            // Check if time-limited session has expired
            timeLimitedSession?.let { session ->
                val elapsedMinutes = ((currentTime - session.startTime) / 60000).toInt()
                Log.d(TAG, "Session check - elapsed: $elapsedMinutes, limit: ${session.timeLimitMinutes}")
                if (elapsedMinutes >= session.timeLimitMinutes) {
                    Log.e(TAG, "Time limit expired for ${session.packageName}")
                    showTimeExpiredScreen(session)
                    timeLimitedSession = null
                    updateNotification()
                    // Mark as recently shown to prevent DelayActivity from appearing
                    session.packageName?.let { pkg ->
                        recentlyShownApps.add(pkg)
                        lastCheckedApp = pkg
                        // Clear after 60 seconds
                        handler?.postDelayed(
                            {
                                recentlyShownApps.remove(pkg)
                                if (lastCheckedApp == pkg) {
                                    lastCheckedApp = null
                                }
                            },
                            60000
                        )
                    }
                }
            }

            // If user switched to a different app, end time-limited session
            if (recentApp != null && timeLimitedSession != null &&
                timeLimitedSession!!.packageName != recentApp.packageName) {
                Log.e(TAG, "User switched app from ${timeLimitedSession!!.packageName} to ${recentApp.packageName}, ending time-limited session")
                timeLimitedSession = null
                updateNotification()
            }

            // If user switched to a non-monitored app, end time-limited session
            if (recentApp != null && !selectedApps.contains(recentApp.packageName)) {
                Log.e(TAG, "User switched to non-monitored app ${recentApp.packageName}, ending time-limited session")
                timeLimitedSession = null
                updateNotification()
            }

            // Check if there's an active time-limited session for current app
            val hasActiveTimeLimit = timeLimitedSession != null &&
                recentApp != null &&
                timeLimitedSession!!.packageName == recentApp.packageName
            Log.d(TAG, "hasActiveTimeLimit: $hasActiveTimeLimit, lastCheckedApp: $lastCheckedApp, isDelayScreenActive: $isDelayScreenActive")

            if (recentApp != null &&
                selectedApps.contains(recentApp.packageName) &&
                !recentlyShownApps.contains(recentApp.packageName) &&
                recentApp.packageName != lastCheckedApp &&
                !isDelayScreenActive &&
                !hasActiveTimeLimit
            ) {
                Log.d(TAG, "SHOWING POPUP for ${recentApp.packageName}")
                // Mark as recently shown and set active flag
                recentlyShownApps.add(recentApp.packageName)
                lastCheckedApp = recentApp.packageName
                isDelayScreenActive = true

                // Show delay screen
                showDelayScreen(recentApp.packageName)

                // Clear recentlyShown after 60 seconds to prevent immediate re-triggers
                handler?.postDelayed(
                    {
                        recentlyShownApps.remove(recentApp.packageName)
                        if (lastCheckedApp == recentApp.packageName) {
                            lastCheckedApp = null
                        }
                    },
                    60000
                )
            } else {
                if (recentApp != null && selectedApps.contains(recentApp.packageName)) {
                    Log.d(TAG, "Popup NOT shown for ${recentApp.packageName} - inRecentlyShown: ${recentlyShownApps.contains(recentApp.packageName)}, isLastChecked: ${recentApp.packageName == lastCheckedApp}, delayActive: $isDelayScreenActive, hasTimeLimit: $hasActiveTimeLimit")
                }
            }

            // Update countdown notification if there's an active time-limited session
            if (timeLimitedSession != null) {
                Log.d(TAG, "Updating notification for active session: ${timeLimitedSession!!.appName}")
                updateNotification()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for app launch: ${e.message}", e)
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val gson = com.google.gson.Gson()
            val stringArray = gson.fromJson(json, Array<String>::class.java)
            stringArray.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
            emptyList()
        }
    }

    private fun parseJsonToMap(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) {
            return emptyMap()
        }
        return try {
            val result = mutableMapOf<String, String>()
            val jsonObject = org.json.JSONObject(json)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getString(key)
                result[key] = value
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON map: ${e.message}")
            emptyMap()
        }
    }

    private fun showDelayScreen(packageName: String) {
        try {
            // Get app name to pass to DelayActivity
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val appNamesJson = prefs.getString("flutter.app_names", null)
            Log.d(TAG, "app_names JSON: $appNamesJson")
            val appNames = parseJsonToMap(appNamesJson)
            val appName = appNames[packageName] ?: packageName
            Log.d(TAG, "Resolved app name: $appName for package: $packageName")

            val intent =
                Intent(this, DelayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("packageName", packageName)
                    putExtra("appName", appName)
                }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delay screen: ${e.message}", e)
        }
    }

    private fun showTimeExpiredScreen(session: SessionData) {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val timeExpiredMessage = prefs.getString("flutter.time_expired_message", "Your time is up! How much more time do you need?")

            val intent =
                Intent(this, TimeExpiredActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("packageName", session.packageName)
                    putExtra("appName", session.appName)
                    putExtra("message", timeExpiredMessage)
                }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing time expired screen: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun onDelayScreenFinished(packageName: String?, appName: String?, timeLimitMinutes: Int, isStayingMindful: Boolean) {
        Log.d(TAG, "=== onDelayScreenFinished called directly: packageName=$packageName, appName=$appName, timeLimit=$timeLimitMinutes, isStayingMindful=$isStayingMindful ===")

        isDelayScreenActive = false

        if (packageName != null && timeLimitMinutes > 0 && appName != null) {
            timeLimitedSession = SessionData(
                startTime = System.currentTimeMillis(),
                timeLimitMinutes = timeLimitMinutes,
                packageName = packageName,
                appName = appName
            )
            Log.e(TAG, "CREATED time-limited session: appName=$appName, timeLimit=$timeLimitMinutes, package=$packageName")
            updateNotification()
            Log.e(TAG, "Started time-limited session for $appName: $timeLimitMinutes minutes")
        } else {
            Log.d(TAG, "No time limit selected, not creating session. timeLimitMinutes=$timeLimitMinutes, appName=$appName, packageName=$packageName")
        }

        if (isStayingMindful) {
            lastCheckedApp?.let { app ->
                Log.e(TAG, "User stayed mindful, not starting session for: $app")
            }
        }

        Log.d(TAG, "Resetting lastCheckedApp from $lastCheckedApp to null")
        lastCheckedApp = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        instance = null
        stopMonitoring()
        unregisterDelayScreenReceiver()
    }

    private val delayScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e(TAG, "=== BROADCAST RECEIVED: ${intent?.action}, all actions: ${intent?.action} ===")
            Log.e(TAG, "=== Intent extras: packageName=${intent?.getStringExtra("packageName")}, timeLimit=${intent?.getIntExtra("timeLimitMinutes", -1)} ===")
            if (intent?.action == "com.example.extra_mind_time.DELAY_SCREEN_FINISHED") {
                isDelayScreenActive = false

                // When user clicks continue, get session data
                val packageName = intent.getStringExtra("packageName")
                val appName = intent.getStringExtra("appName")
                val timeLimitMinutes = intent.getIntExtra("timeLimitMinutes", 0)

                Log.d(TAG, "Delay screen finished - Package: $packageName, App: $appName, TimeLimit: $timeLimitMinutes")

                // If user selected a time limit, start time-limited session
                if (packageName != null && timeLimitMinutes > 0 && appName != null) {
                    timeLimitedSession = SessionData(
                        startTime = System.currentTimeMillis(),
                        timeLimitMinutes = timeLimitMinutes,
                        packageName = packageName,
                        appName = appName
                    )
                    Log.e(TAG, "CREATED time-limited session: appName=$appName, timeLimit=$timeLimitMinutes, package=$packageName")
                    updateNotification()
                    Log.e(TAG, "Started time-limited session for $appName: $timeLimitMinutes minutes")
                } else {
                    Log.d(TAG, "No time limit selected, not creating session. timeLimitMinutes=$timeLimitMinutes, appName=$appName, packageName=$packageName")
                }

                // Also handle when user chooses to stay mindful
                val isStayingMindful = intent.getBooleanExtra("isStayingMindful", false)
                if (isStayingMindful) {
                    lastCheckedApp?.let { app ->
                        // Don't start a session if user stayed mindful
                        Log.e(TAG, "User stayed mindful, not starting session for: $app")
                    }
                }

                // Reset lastCheckedApp so other apps can trigger popups
                Log.d(TAG, "Resetting lastCheckedApp from $lastCheckedApp to null")
                lastCheckedApp = null
            }
        }
    }

    private val timeExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "timeExpiredReceiver received: ${intent?.action}")
            when (intent?.action) {
                "com.example.extra_mind_time.EXTEND_TIME" -> {
                    // Extend time by 1 minute from notification
                    Log.d(TAG, "EXTEND_TIME received")
                    timeLimitedSession?.apply {
                        timeLimitMinutes += 1
                        Log.e(TAG, "EXTENDED time for $appName by 1 minute: $timeLimitMinutes minutes total")
                        updateNotification()
                    } ?: run {
                        Log.e(TAG, "Cannot extend - no active session")
                    }
                }
                "com.example.extra_mind_time.TIME_LIMIT_EXTENDED" -> {
                    // User extended time from TimeExpiredActivity
                    val packageName = intent.getStringExtra("packageName")
                    val appName = intent.getStringExtra("appName")
                    val extraMinutes = intent.getIntExtra("extraMinutes", 0)

                    if (packageName != null && appName != null && extraMinutes > 0) {
                        timeLimitedSession?.apply {
                            timeLimitMinutes = extraMinutes
                            startTime = System.currentTimeMillis()
                            Log.e(TAG, "EXTENDED time for $appName: $timeLimitMinutes minutes")
                            updateNotification()
                        }
                    }
                }
            }
        }
    }

    private fun registerDelayScreenReceiver() {
        Log.d(TAG, "Registering delayScreenReceiver")
        try {
            val filter = IntentFilter("com.example.extra_mind_time.DELAY_SCREEN_FINISHED")
            // Don't use RECEIVER_NOT_EXPORTED for same-app broadcasts
            registerReceiver(delayScreenReceiver, filter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "delayScreenReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering delayScreenReceiver: ${e.message}", e)
        }

        try {
            val timeExpiredFilter = IntentFilter().apply {
                addAction("com.example.extra_mind_time.EXTEND_TIME")
                addAction("com.example.extra_mind_time.TIME_LIMIT_EXTENDED")
            }
            registerReceiver(timeExpiredReceiver, timeExpiredFilter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "timeExpiredReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering timeExpiredReceiver: ${e.message}", e)
        }
    }

    private fun unregisterDelayScreenReceiver() {
        try {
            Log.d(TAG, "Unregistering delayScreenReceiver")
            unregisterReceiver(delayScreenReceiver)
            Log.d(TAG, "delayScreenReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering delayScreenReceiver: ${e.message}")
        }
        try {
            Log.d(TAG, "Unregistering timeExpiredReceiver")
            unregisterReceiver(timeExpiredReceiver)
            Log.d(TAG, "timeExpiredReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering timeExpiredReceiver: ${e.message}")
        }
    }
}
