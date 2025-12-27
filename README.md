# Extra Mind Time - Android App Usage Manager

A Flutter app for Android that helps you be more intentional with your app usage by showing a delay screen when you open selected apps.

## Features

- 🔒 **Permission Management**: Easy-to-use interface for requesting necessary Android permissions
- 📱 **App Selection**: Choose which apps you want to monitor from your installed applications
- ⏱️ **Customizable Delay**: Set delay duration from 1 to 30 seconds
- 💭 **Mindful Messages**: Customize or choose from default mindful messages
🎨 **Beautiful UI**: Modern Material 3 design with smooth animations
🔄 **Real-time Monitoring**: Background service monitors selected app launches

## How It Works

1. **Select Apps**: Choose which apps you want to add an intentional pause to
2. **Configure Settings**: Set your delay duration and choose a message
3. **Start Monitoring**: Enable the monitoring service
4. **Intentional Pause**: When you open a selected app, you'll see a beautiful delay screen with your chosen message and a countdown timer

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
- Set via the Settings screen
- Uses a smooth slider interface

### Messages

Choose from default messages or create your own:
- "Take a moment to breathe and be present."
- "Is this truly necessary right now?"
- "Remember your intentions for today."
- "What matters most to you in this moment?"
- "Take a deep breath before continuing."
- "Are you choosing this intentionally?"
- "Stay present. Stay focused."
- "This moment is all you have."

## Technical Details

### Dependencies

- `permission_handler` - Handle Android permissions
- `usage_stats` - Monitor app usage statistics
- `shared_preferences` - Store app settings locally
- `device_apps` - List installed applications

### Architecture

```
lib/
├── main.dart                 # App entry point and initialization
├── screens/
│   ├── permissions_screen.dart   # Permission request UI
│   ├── home_screen.dart          # Main dashboard
│   ├── app_selection_screen.dart # App selection interface
│   ├── settings_screen.dart      # Settings configuration
│   └── delay_screen.dart         # Intentional delay screen
├── services/
│   └── app_monitor_service.dart  # Background monitoring service
└── models/
    └── (future model classes)
```

### Key Components

1. **AppMonitorService**: Periodically checks for app launches and triggers the delay screen
2. **DelayScreen**: Full-screen overlay with countdown timer and intentional message
3. **PermissionsScreen**: Guides users through permission setup
4. **HomeScreen**: Central dashboard for monitoring control and configuration

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

1. **Start Small**: Begin with 1-2 apps that you want to be more mindful about
- **Adjust Duration**: Find a delay time that works for you - not too short, not too long
- **Personalize Messages**: Create messages that resonate with your personal goals
- **Regular Review**: Periodically review and adjust your selected apps

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

## Future Enhancements

- [ ] Statistics and usage insights
- [ ] Daily/weekly usage reports
- [ ] Custom delay times per app
- [ ] Multiple message rotation
- [ ] Widget support
- [ ] Export/import settings
- [ ] Dark mode theme

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or suggestions, please open an issue on the GitHub repository.

---

**Note**: This app is designed to run on Android only and requires minimum API level 22 (Android 5.1).