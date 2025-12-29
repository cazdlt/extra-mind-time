package com.example.extra_mind_time

import android.app.*
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
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
    private val TIME_EXPIRATION_REQUEST_CODE = 1002
    private var handler: Handler? = null
    private var monitoringRunnable: Runnable? = null
    private var lastProcessedEventTime: Long = 0
    private var isDelayScreenActive = false
    var timeLimitedSession: SessionData? = null
    private var notificationUpdateRunnable: Runnable? = null
    private var alarmManager: AlarmManager? = null
    private var timeExpirationPendingIntent: PendingIntent? = null

    companion object {
        const val ACTION_START_MONITORING = "com.example.extra_mind_time.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.extra_mind_time.STOP_MONITORING"
        private const val CHECK_INTERVAL = 2000L // Check for new events every 2 seconds
        private const val NOTIFICATION_UPDATE_INTERVAL = 1000L

        @Volatile
        private var instance: AppMonitorService? = null

        fun getInstance(): AppMonitorService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        notificationUpdateRunnable = object : Runnable {
            override fun run() {
                if (timeLimitedSession != null) {
                    updateNotification()
                }
                handler?.postDelayed(this, NOTIFICATION_UPDATE_INTERVAL)
            }
        }

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

    fun updateNotification() {
        Log.d(TAG, "updateNotification called")
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "updateNotification completed - notification ID: $NOTIFICATION_ID")
    }

    private fun startMonitoring() {
        lastProcessedEventTime = System.currentTimeMillis()
        stopMonitoring()

        monitoringRunnable =
            object : Runnable {
                override fun run() {
                    checkForAppLaunch()
                    handler?.postDelayed(this, CHECK_INTERVAL)
                }
            }

        handler?.post(monitoringRunnable!!)
        handler?.post(notificationUpdateRunnable!!)
    }

    private fun stopMonitoring() {
        Log.d(TAG, "=== stopMonitoring called ===")
        monitoringRunnable?.let { handler?.removeCallbacks(it) }
        monitoringRunnable = null
        notificationUpdateRunnable?.let { handler?.removeCallbacks(it) }
        cancelTimeExpirationAlarm()
        isDelayScreenActive = false
        timeLimitedSession = null
        updateNotification()
        unregisterDelayScreenReceiver()
    }

    private fun setTimeExpirationAlarm(timeLimitMinutes: Int) {
        cancelTimeExpirationAlarm()

        val expirationTime = System.currentTimeMillis() + (timeLimitMinutes * 60 * 1000L)

        val intent = Intent("com.example.extra_mind_time.TIME_EXPIRED")
        timeExpirationPendingIntent = PendingIntent.getBroadcast(
            this,
            TIME_EXPIRATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    expirationTime,
                    timeExpirationPendingIntent!!
                )
            } else {
                it.setExact(
                    AlarmManager.RTC_WAKEUP,
                    expirationTime,
                    timeExpirationPendingIntent!!
                )
            }
            Log.d(TAG, "Time expiration alarm set for $timeLimitMinutes minutes from now")
        }
    }

    fun cancelTimeExpirationAlarm() {
        timeExpirationPendingIntent?.let {
            alarmManager?.cancel(it)
            Log.d(TAG, "Time expiration alarm cancelled")
        }
        timeExpirationPendingIntent = null
    }

    private fun checkForAppLaunch() {
        try {
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val selectedAppsJson = prefs.getString("flutter.selected_apps", null)

            if (selectedAppsJson.isNullOrEmpty() || selectedAppsJson == "[]") {
                return
            }

            val selectedApps = parseJsonArray(selectedAppsJson)

            if (selectedApps.isEmpty()) {
                return
            }

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()

            val usageEvents = usageStatsManager.queryEvents(lastProcessedEventTime, currentTime)

            if (usageEvents == null) {
                return
            }

            var currentForegroundPackage: String? = null
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                val packageName = event.packageName
                val eventType = event.eventType
                val eventTime = event.timeStamp

                if (packageName == this.packageName) {
                    continue
                }

                if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    Log.d(TAG, "App moved to foreground: $packageName at $eventTime")
                    currentForegroundPackage = packageName

                    if (selectedApps.contains(packageName)) {
                        val hasActiveTimeLimit = timeLimitedSession != null &&
                            timeLimitedSession!!.packageName == packageName

                        if (!isDelayScreenActive && !hasActiveTimeLimit) {
                            Log.d(TAG, "SHOWING POPUP for $packageName")
                            isDelayScreenActive = true
                            showDelayScreen(packageName)
                        } else if (hasActiveTimeLimit) {
                            Log.d(TAG, "Popup NOT shown - active time limit for $packageName")
                        } else if (isDelayScreenActive) {
                            Log.d(TAG, "Popup NOT shown - delay screen active for $packageName")
                        }
                    }
                } else if (eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    Log.d(TAG, "App moved to background: $packageName at $eventTime")

                    timeLimitedSession?.let { session ->
                        if (session.packageName == packageName) {
                            Log.e(TAG, "User switched from ${session.packageName}, ending time-limited session")
                            cancelTimeExpirationAlarm()
                            timeLimitedSession = null
                            updateNotification()
                        }
                    }
                }

                if (eventTime > lastProcessedEventTime) {
                    lastProcessedEventTime = eventTime
                }
            }

            if (currentForegroundPackage != null && !selectedApps.contains(currentForegroundPackage)) {
                timeLimitedSession?.let { session ->
                    if (session.packageName != currentForegroundPackage) {
                        Log.e(TAG, "User switched to non-monitored app, ending time-limited session")
                        cancelTimeExpirationAlarm()
                        timeLimitedSession = null
                        updateNotification()
                    }
                }
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

    fun showTimeExpiredScreen(session: SessionData) {
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
            setTimeExpirationAlarm(timeLimitMinutes)
            Log.e(TAG, "CREATED time-limited session: appName=$appName, timeLimit=$timeLimitMinutes, package=$packageName")
            updateNotification()
            Log.e(TAG, "Started time-limited session for $appName: $timeLimitMinutes minutes")
        } else {
            Log.d(TAG, "No time limit selected, not creating session. timeLimitMinutes=$timeLimitMinutes, appName=$appName, packageName=$packageName")
        }

        if (isStayingMindful) {
            Log.e(TAG, "User stayed mindful, not starting session")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        instance = null
        stopMonitoring()
        unregisterDelayScreenReceiver()
    }

    private val clearStuckStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.extra_mind_time.CLEAR_STUCK_STATE") {
                isDelayScreenActive = false
                Log.d(TAG, "Stuck state cleared via broadcast")
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
                        setTimeExpirationAlarm(timeLimitMinutes)
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
                            setTimeExpirationAlarm(timeLimitMinutes)
                            Log.e(TAG, "EXTENDED time for $appName: $timeLimitMinutes minutes")
                            updateNotification()
                        }
                    }
                }
            }
        }
    }

    private fun registerDelayScreenReceiver() {
        try {
            val clearStuckFilter = IntentFilter("com.example.extra_mind_time.CLEAR_STUCK_STATE")
            registerReceiver(clearStuckStateReceiver, clearStuckFilter, Context.RECEIVER_EXPORTED)
            Log.d(TAG, "clearStuckStateReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering clearStuckStateReceiver: ${e.message}", e)
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

        Log.d(TAG, "TimeExpirationReceiver registered in manifest")
    }

    private fun unregisterDelayScreenReceiver() {
        try {
            unregisterReceiver(clearStuckStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering clearStuckStateReceiver: ${e.message}")
        }
        try {
            Log.d(TAG, "Unregistering timeExpiredReceiver")
            unregisterReceiver(timeExpiredReceiver)
            Log.d(TAG, "timeExpiredReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering timeExpiredReceiver: ${e.message}")
        }
        Log.d(TAG, "TimeExpirationReceiver is registered in manifest, no need to unregister")
    }
}

class TimeExpirationReceiver : BroadcastReceiver() {
    private val TAG = "TimeExpirationReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.example.extra_mind_time.TIME_EXPIRED") {
            Log.d(TAG, "Time expiration alarm fired")

            val service = AppMonitorService.getInstance()
            service?.let {
                it.timeLimitedSession?.let { session ->
                    Log.e(TAG, "Time limit expired for ${session.packageName}")
                    it.cancelTimeExpirationAlarm()
                    it.showTimeExpiredScreen(session)
                    it.timeLimitedSession = null
                    it.updateNotification()
                } ?: run {
                    Log.e(TAG, "No active session when time expiration fired")
                }
            }
        }
    }
}
