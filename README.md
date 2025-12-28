# Extra Mind Time - Mindful App Usage Manager

A Flutter app for Android that helps you be more intentional with your app usage by showing a mindful delay screen when you open selected apps, followed by configurable time limits.

## Features

- 🔒 **Permission Management**: Easy-to-use interface for requesting necessary Android permissions
- 📱 **App Selection**: Choose which apps you want to monitor from your installed applications
- 🧘 **Mindful Delay**: 5-second countdown with customizable message before opening apps
- ⏱️ **Time Limits**: Select time limits (2, 5, 10, 15, 20, or 30 minutes) for monitored apps
- 📊 **Real-time Countdown**: Persistent notification shows remaining time in minutes:seconds format
- ➕ **Easy Extensions**: Add +1 minute to your time limit directly from the notification
- 🎨 **Customizable UI**: Set background colors and personalized messages
- 🔄 **Smart Session Management**: Automatically ends sessions when switching apps
- 🎯 **Kind Reminders**: Gentle popup when time expires with extension options

## How It Works

1. **Select Apps**: Choose which apps you want to add mindful limits to
2. **Configure Settings**: Set your delay duration, time limit options, and custom messages
3. **Start Monitoring**: Enable the monitoring service
4. **Mindful Pause**: When you open a selected app, you'll see a 5-second delay screen with your chosen message
5. **Choose Time Limit**: After the pause, select how long you want to use the app
6. **Track Time**: A notification shows your remaining time in minutes:seconds format
7. **Extend or End**: Add more time or switch apps to end your session

## Permissions Required

The app requires the following Android permissions:

- **Usage Stats Access**: To detect when selected apps are opened
- **Display Over Other Apps**: To show the delay screen over other applications
- **Notifications**: To run the background monitoring service

All permissions are requested through the app's user-friendly interface.

## Installation

### Prerequisites

- Flutter SDK (^3.9.2)
- Android SDK (API level 22 or higher)
- Android device or emulator

### Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd extra_mind_time
```

2. Install dependencies:
```bash
flutter pub get
```

3. Run the app:
```bash
flutter run
```

Or use the provided script:
```bash
./run_app.sh
```

### Installing on Physical Device (Motorola/Android)

1. **Enable Developer Options** on your phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times until it says "You are now a developer"

2. **Enable USB Debugging**:
   - Go to Settings → System → Developer Options
   - Enable "USB Debugging"

3. **Connect your phone** via USB cable

4. **Install the release build**:
```bash
./scripts/install_on_phone.sh
```

Or manually:
```bash
flutter build apk --release
flutter install
```

**Note**: The app will appear as "Extra Mind Time" in your app drawer. All permissions will be requested when you first launch the app.

## Configuration

### Delay Duration
- Adjustable from 1 to 30 seconds
- Default: 5 seconds for mindful pause
- Set via the Settings screen with a smooth slider interface

### Time Limit Options
- Choose up to 3 time limit options from: 2, 5, 10, 15, 20, 30 minutes
- Default options: 2, 5, 10 minutes
- Users select their desired limit after the mindful pause

### Messages
- **Mindful Message**: Displayed during the delay screen (max 200 characters)
- **Time Expired Message**: Shown when time runs out (max 200 characters)
  - Default: "Your mindful time is complete. How much more time would you like?"
- Create custom messages that resonate with your goals

### Background Colors
- Choose from 10 preset colors for the mindful delay screen
- Options: Deep Purple, Indigo, Blue, Teal, Green, Orange, Red, Pink, Dark Grey, Black

## Technical Details

### Dependencies

- `permission_handler` - Handle Android permissions
- `shared_preferences` - Store app settings locally
- `device_apps` - List installed applications

### Architecture

```
lib/
├── main.dart                 # App entry point and initialization
├── screens/
│   ├── permissions_screen.dart    # Permission request UI
│   ├── home_screen.dart           # Main dashboard
│   ├── app_selection_screen.dart  # App selection interface
│   └── settings_screen.dart       # Settings configuration
├── services/
│   └── app_monitor_service.dart   # Flutter service interface

android/app/src/main/kotlin/com/example/extra_mind_time/
├── AppMonitorService.kt        # Background monitoring service
├── DelayActivity.kt            # Mindful delay screen (native)
└── TimeExpiredActivity.kt      # Time limit expired popup (native)
```

### Key Components

1. **AppMonitorService** (Kotlin): Foreground service that:
   - Monitors app launches every 2 seconds
   - Shows DelayActivity when monitored apps are opened
   - Manages time-limited sessions with countdown
   - Updates notification with remaining time (MM:SS format)
   - Handles +1 minute extension from notification
   - Ends sessions when switching to other apps

2. **DelayActivity** (Kotlin): Full-screen overlay that:
   - Displays customizable mindful message
   - Shows 5-second countdown timer
   - Presents time limit options after countdown
   - Calls service directly when user selects time limit

3. **TimeExpiredActivity** (Kotlin): Popup shown when time expires:
   - Displays custom expiration message
   - Offers extension options (2, 5, 10, 15, 20, 30 minutes)
   - Allows returning to home screen

4. **HomeScreen** (Flutter): Central dashboard for:
   - Selecting apps to monitor
   - Starting/stopping monitoring
   - Accessing settings

5. **SettingsScreen** (Flutter): Configuration for:
   - Delay duration (1-30 seconds)
   - Time limit options (select up to 3)
   - Mindful message customization
   - Time expired message customization
   - Background color selection

### Session Management

- **Single Session**: Only one time-limited session active at a time
- **Auto-End**: Session ends when user switches to a different app
- **Countdown Format**: Notification shows `MM:SS` (e.g., "2:30 remaining")
- **Extension**: +1 minute button available in notification
- **Expiration Prevention**: After time expires, app is marked to prevent DelayActivity from reappearing

## Android Manifest Configuration

The app requires the following manifest entries:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
```

## Usage Tips

1. **Start Small**: Begin with 1-2 apps you want to be more mindful about (social media, games, etc.)
2. **Choose Limits Wisely**: Start with longer time limits (15-30 min) and gradually reduce
3. **Personalize Messages**: Create messages that resonate with your personal goals
4. **Use Extensions Sparingly**: The +1 minute button is there for when you genuinely need it
5. **Review Regularly**: Periodically review your selected apps and time limits
6. **Be Kind to Yourself**: The app is about mindfulness, not restriction

## Troubleshooting

### Permissions Not Working
- Ensure all permissions are granted in Android Settings
- Try restarting the monitoring service
- Restart the app if permissions were just granted

### App Not Detecting Launches
- Verify Usage Stats permission is enabled
- Check that monitoring is turned ON in the app
- Ensure battery optimization is disabled for the app

### Delay Screen Not Showing
- Confirm Display Over Other Apps permission is granted
- Make sure the app is selected for monitoring
- Check that monitoring service is running

### Countdown Notification Not Updating
- Swipe away and re-enable monitoring to restart the service
- Check that the app is still in the foreground
- Ensure you haven't switched to a different monitored app

### Time Expired Popup Not Appearing
- Verify you haven't switched apps (session ends on app switch)
- Check that notification permissions are enabled
- Make sure the app hasn't been killed by the system

### Both Popups Appearing
- After time expires, the app prevents the DelayActivity for 60 seconds
- If you see both, force stop the app and restart monitoring

### Session Ending Unexpectedly
- Sessions automatically end when you switch to another app
- This is intentional - one time-limited session at a time
- Re-open the app to start a new session

## Future Enhancements

### Free Tier (Current)
- 2 monitored apps maximum
- Mindful delay with countdown
- Time limit options (2, 5, 10 minutes)
- Custom messages and colors
- Real-time countdown notifications

### Premium Tier ($1.99 one-time purchase)
- Unlimited monitored apps
- Usage reports and analytics (time spent per app, sessions per day, trends)
- Widget support for quick monitoring toggle
- Premium-only time limit options (custom any duration)
- Export/import settings

### Implementation Roadmap
- [ ] Integrate Google Play Billing Library
- [ ] Create premium upgrade missing features
- [ ] Implement usage analytics data collection
- [ ] Create home screen widget for monitoring toggle

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or suggestions, please open an issue on the GitHub repository.

---

**Note**: This app is designed to run on Android only and requires minimum API level 22 (Android 5.1).
