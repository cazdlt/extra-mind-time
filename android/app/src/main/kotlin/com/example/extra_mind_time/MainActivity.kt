package com.example.extra_mind_time

import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.extra_mind_time/monitor"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call,
                                                                                                  result ->
            when (call.method) {
                "startMonitoring" -> {
                    try {
                        startMonitoringService()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SERVICE_ERROR", e.message, null)
                    }
                }

                "stopMonitoring" -> {
                    try {
                        stopMonitoringService()
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SERVICE_ERROR", e.message, null)
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun startMonitoringService() {
        val intent =
            Intent(this, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_START_MONITORING
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopMonitoringService() {
        val intent =
            Intent(this, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_STOP_MONITORING
            }
        startService(intent)
    }
}
