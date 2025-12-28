import 'dart:async';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppMonitorService {
  static const platform = MethodChannel('com.example.extra_mind_time/monitor');
  static bool _isMonitoring = false;

  static Future<bool> startMonitoring() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final appsJson = prefs.getString('selected_apps');
      final delaySeconds = prefs.getInt('delay_seconds') ?? 5;

      if (appsJson == null || appsJson == '[]') {
        return false;
      }

      if (delaySeconds < 1 || delaySeconds > 30) {
        return false;
      }

      final result = await platform.invokeMethod('startMonitoring');

      if (result == true) {
        _isMonitoring = true;
        return true;
      } else {
        return false;
      }
    } catch (e) {
      return false;
    }
  }

  static Future<void> stopMonitoring() async {
    try {
      await platform.invokeMethod('stopMonitoring');
      _isMonitoring = false;
    } catch (e) {
      // Silently handle error
    }
  }

  static bool isMonitoring() {
    return _isMonitoring;
  }
}
