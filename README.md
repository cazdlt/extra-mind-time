# Extra Mind Time - Mindful App Usage Manager

A Flutter app for Android that helps you be more intentional with your app usage by showing a mindful delay screen when you open selected apps, followed by configurable time limits. Optimized for Android 15 and high reliability.

## Features

- 🔒 **Permission Management**: Easy-to-use interface for requesting necessary Android permissions, including Exact Alarms and Usage Stats.
- 📱 **App Selection**: Choose which apps you want to monitor from your installed applications.
- 🧘 **Mindful Delay**: customizable countdown with a mindful message before opening apps.
- ⏱️ **Time Limits**: Select time limits (2, 5, 10, 15, 20, or 30 minutes) for monitored apps.
- 📊 **Real-time Countdown**: Persistent notification shows remaining time in `minutes:seconds` format.
- ➕ **Quick Extensions**: Add +1 minute to your time limit directly from the notification.
- 🎨 **Customizable UI**: Set background colors and personalized messages for delay and expiration screens.
- 🔄 **Smart Session Management**: Automatically ends sessions when switching apps to ensure you stay mindful.
- 🎯 **Reliable Monitoring**: Foreground service with boot-start and crash-recovery support.

## How It Works

1. **Select Apps**: Choose which apps you want to add mindful limits to.
2. **Configure Settings**: Set your delay duration, time limit options, and custom messages.
3. **Start Monitoring**: Enable the monitoring service.
4. **Mindful Pause**: When you open a selected app, you'll see a delay screen with your chosen message.
5. **Choose Time Limit**: After the pause, select how long you want to use the app.
6. **Track Time**: A notification shows your remaining time in `MM:SS` format.
7. **Extend or End**: Add more time or switch apps to end your session.

## Permissions Required

The app requires the following Android permissions:

- **Usage Stats Access**: To detect when selected apps are opened.
- **Display Over Other Apps**: To show the delay screen over other applications.
- **Notifications**: To run the background monitoring service and show countdowns.
- **Exact Alarms**: To ensure time limits expire precisely at the right moment (especially on Android 12+).
- **Run at Startup**: To resume monitoring automatically after a device reboot.

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
./scripts/run_app.sh
```

## Technical Details

### Architecture

- **Flutter (UI)**: Handles settings, app selection, and permission logic.
- **Kotlin (Service)**: A robust foreground service (`AppMonitorService`) uses `UsageStatsManager` to track app launches and `AlarmManager` for precise time limit enforcement.
- **Native Activities**: `DelayActivity` and `TimeExpiredActivity` provide the overlay UI, communicating directly with the service for high reliability.

### Key Reliability Improvements

- **Android 15 Ready**: Complies with API 34/35 requirements for foreground services (`specialUse`) and exact alarm scheduling.
- **Persistence**: Handles service restarts gracefully and includes a `BootReceiver` for persistence across reboots.
- **Smart Logic**: 1000ms check interval with look-back logic ensures app launches are detected even if the system is under load, while preventing duplicate popups.

## License

This project is open source and available under the MIT License.

---

**Note**: This app is designed for Android and requires minimum API level 22 (Android 5.1).
