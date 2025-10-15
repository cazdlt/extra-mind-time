import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppMonitorService {
  static const platform = MethodChannel('com.example.extra_mind_time/monitor');
  static bool _isMonitoring = false;

  static Future<bool> startMonitoring() async {
    try {
      // Check if we have the necessary settings
      final prefs = await SharedPreferences.getInstance();
      final appsJson = prefs.getString('selected_apps');

      if (appsJson == null || appsJson == '[]') {
        debugPrint('AppMonitorService: No apps selected');
        return false;
      }

      // Call native method to start the foreground service
      final result = await platform.invokeMethod('startMonitoring');

      if (result == true) {
        _isMonitoring = true;
        debugPrint('AppMonitorService: Native monitoring service started');
        return true;
      } else {
        debugPrint('AppMonitorService: Failed to start native service');
        return false;
      }
    } catch (e) {
      debugPrint('AppMonitorService: Error starting monitoring: $e');
      return false;
    }
  }

  static Future<void> stopMonitoring() async {
    try {
      // Call native method to stop the foreground service
      await platform.invokeMethod('stopMonitoring');
      _isMonitoring = false;
      debugPrint('AppMonitorService: Native monitoring service stopped');
    } catch (e) {
      debugPrint('AppMonitorService: Error stopping monitoring: $e');
    }
  }

  static bool isMonitoring() {
    return _isMonitoring;
  }
}
