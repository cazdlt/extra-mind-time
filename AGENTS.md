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
- **Services**: `AppMonitorService.kt` (periodic checks), `DelayActivity.kt` (delay screen), `TimeExpiredActivity.kt` (time expired screen)

## Critical Patterns

### SharedPreferences (Flutter prefix)
```dart
// Flutter save
prefs.setString('selected_apps', json.encode(_selectedApps));
prefs.setString('app_names', json.encode(_appNames));
prefs.setInt('delay_seconds', _delaySeconds);
prefs.setString('mindful_message', _mindfulMessage);
prefs.setInt('background_color', _backgroundColor.value);
prefs.setString('time_limit_options', json.encode(_timeLimitOptions));
prefs.setString('time_expired_message', _timeExpiredMessage);
prefs.setBool('is_monitoring', _isMonitoring);

// Kotlin read (must include "flutter." prefix)
prefs.getString("flutter.selected_apps", null)
prefs.getString("flutter.app_names", null)
prefs.getLong("flutter.delay_seconds", 5L)
prefs.getString("flutter.mindful_message", null)
prefs.getLong("flutter.background_color", -7637753)
prefs.getString("flutter.time_limit_options", null)
prefs.getString("flutter.time_expired_message", null)
```

### MethodChannel Methods
- `startMonitoring`: Starts foreground service
- `stopMonitoring`: Stops foreground service

### Session Tracking
- User selects time limit → time-limited session starts
- User switches to different app → current session ends, new app can start session
- User switches to non-monitored app → session ends
- User clicks "Stay Mindful" → no session started
- Time limit expires → TimeExpiredActivity shows, user can extend or stop

### Time Limit Flow
1. User opens monitored app → mindful delay screen shows
2. After countdown → user selects time limit (2/5/10 min)
3. If limit selected → countdown notification shows with remaining time
4. User can extend time by clicking "+1 min" in notification
5. When time expires → TimeExpiredActivity shows with same time options
6. User can extend time or stop using app
7. Only ONE time-limited session active at a time (switching apps resets timer)

### Broadcast Communication
- `DELAY_SCREEN_FINISHED`: Delay screen finished
  - Extras: `packageName`, `appName`, `timeLimitMinutes`, `isStayingMindful`
  - Action: If `timeLimitMinutes > 0`, creates time-limited session and shows countdown notification
- `TIME_LIMIT_EXTENDED`: User extended time from TimeExpiredActivity
  - Extras: `packageName`, `appName`, `extraMinutes`
  - Action: Resets session with new time limit
- `EXTEND_TIME`: User clicked +1 min in countdown notification
  - No extras
  - Action: Adds 1 minute to current time limit

## Code Conventions
- **Dart**: camelCase variables, PascalCase classes, StatefulWidget with setState(), Material 3, no comments
- **Kotlin**: Handler/Looper (not coroutines), `Log.d(TAG, "message")`, no comments

## Git Operations
- **NEVER commit automatically** unless explicitly asked by the user
- Let the user handle all git operations (add, commit, push, etc.) unless specifically instructed
- If you make changes, just complete the task and let the user decide when/what to commit

## Key Constants
- Check interval: 2000ms
- Delay range: 1-30 seconds
- Recently shown cooldown: 60 seconds
- Time limit options: Default [2, 5, 10] minutes (configurable, max 3 options)

## File Structure
```
lib/
  main.dart → screens/ (permissions, home, app_selection, settings) → services/ (app_monitor_service.dart)
android/.../extra_mind_time/
  MainActivity.kt → AppMonitorService.kt → DelayActivity.kt → TimeExpiredActivity.kt
```

## Removed Features
- Re-check interval (replaced by time-limit feature)
- activeAppSessions tracking (replaced by timeLimitedSession)
```

## Common Issues
- **Delay not showing**: Check SYSTEM_ALERT_WINDOW permission, verify monitoring active, check `adb logcat -s AppMonitorService`
- **Monitoring not starting**: Verify all permissions granted, check PACKAGE_USAGE_STATS in Android Settings, disable battery optimization
- **Build failures**: `flutter clean && flutter pub get`

