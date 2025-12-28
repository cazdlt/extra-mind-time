# AGENTS.md - Extra Mind Time Development Guide

Flutter + Kotlin Android app showing delay screens before opening monitored apps. API 22+.

## Quick Commands
```bash
flutter pub get
./scripts/run_app.sh        # Auto-launches emulator
flutter run -d <device_id>  # Manual run
flutter build apk --release # Release build
```

## Tech Stack
- Flutter 3.9.2+ (Dart) + Kotlin (Android native)
- SharedPreferences with `flutter.` prefix for cross-platform storage
- MethodChannel: `com.example.extra_mind_time/monitor` for Flutter↔Kotlin communication

## Architecture
- **Flutter** (`lib/`): UI, settings, app selection
- **Kotlin** (`android/.../extra_mind_time/`): Foreground service, app monitoring, delay overlay
- **Screens**: permissions → home → app_selection/settings
- **Services**: `AppMonitorService.kt` (periodic checks), `DelayActivity.kt` (delay screen)

## Critical Patterns

### SharedPreferences (Flutter prefix)
```dart
// Flutter save
prefs.setString('selected_apps', json.encode(_selectedApps));
prefs.setInt('delay_seconds', _delaySeconds);
prefs.setString('mindful_message', _mindfulMessage);
prefs.setInt('background_color', _backgroundColor.value);
prefs.setInt('recheck_interval_minutes', _recheckIntervalMinutes);
prefs.setBool('is_monitoring', _isMonitoring);

// Kotlin read
prefs.getString("flutter.selected_apps", null)
prefs.getLong("flutter.delay_seconds", 5L)
```

### MethodChannel Methods
- `startMonitoring`: Starts foreground service
- `stopMonitoring`: Stops foreground service

### Session Tracking
- User passes delay → session started (prevents re-trigger)
- User switches to non-monitored app → all sessions cleared
- Recheck interval expires → session cleared (default 30min)
- User clicks "Stay Mindful" → no session started

### Broadcast Communication
- Action: `com.example.extra_mind_time.DELAY_SCREEN_FINISHED`
- Extras: `packageName`, `isStayingMindful`

## Code Conventions
- **Dart**: camelCase variables, PascalCase classes, StatefulWidget with setState(), Material 3, no comments
- **Kotlin**: Handler/Looper (not coroutines), `Log.d(TAG, "message")`, no comments

## Git Operations
- **NEVER commit automatically** unless explicitly asked by the user
- Let the user handle all git operations (add, commit, push, etc.) unless specifically instructed
- If you make changes, just complete the task and let the user decide when/what to commit

## Key Constants
- Check interval: 2000ms
- Default recheck: 30 minutes
- Delay range: 1-30 seconds
- Recently shown cooldown: 60 seconds

## File Structure
```
lib/
  main.dart → screens/ (permissions, home, app_selection, settings) → services/ (app_monitor_service.dart)
android/.../extra_mind_time/
  MainActivity.kt → AppMonitorService.kt → DelayActivity.kt
```

## Common Issues
- **Delay not showing**: Check SYSTEM_ALERT_WINDOW permission, verify monitoring active, check `adb logcat -s AppMonitorService`
- **Monitoring not starting**: Verify all permissions granted, check PACKAGE_USAGE_STATS in Android Settings, disable battery optimization
- **Build failures**: `flutter clean && flutter pub get`

