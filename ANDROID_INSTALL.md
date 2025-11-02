# Android Device Installation Guide

This guide will help you install the Extra Mind Time app on your physical Android device.

## Compatibility

✅ **Fully tested on:**
- Android 15 (API 35)
- Motorola devices

✅ **Supported Android versions:**
- Android 5.1 (API 22) and higher

## Prerequisites

- Android device with **API level 22 (Android 5.1) or higher**
- USB cable to connect your device to your computer
- ADB (Android Debug Bridge) installed on your computer
- The app APK file

## Step 1: Enable Developer Options

1. Open **Settings** on your Android device
2. Scroll down to **About phone** (or **About device**)
3. Find **Build number** (may be under "Software information")
4. Tap **Build number** 7 times
5. You'll see a message: "You are now a developer!"

## Step 2: Enable USB Debugging

1. Go back to main **Settings**
2. Open **Developer options** (usually near the bottom)
3. Enable **USB debugging**
4. Enable **Install via USB** (if available)

## Step 3: Connect Your Device

1. Connect your Android device to your computer via USB
2. On your device, you'll see a prompt: "Allow USB debugging?"
3. Check "Always allow from this computer"
4. Tap **OK**

## Step 4: Verify Connection

Open a terminal/command prompt and run:

```bash
adb devices
```

You should see your device listed:
```
List of devices attached
ABC123DEF456    device
```

If you see "unauthorized", check your phone for the USB debugging prompt.

## Step 5: Build the APK

### Option A: Debug APK (Recommended for testing)

```bash
cd extra_mind_time
flutter build apk --debug
```

The APK will be at: `build/app/outputs/flutter-apk/app-debug.apk`

### Option B: Release APK (Smaller size, optimized)

```bash
flutter build apk --release
```

The APK will be at: `build/app/outputs/flutter-apk/app-release.apk`

## Step 6: Install the APK

### Method 1: Using ADB (Recommended)

```bash
adb install build/app/outputs/flutter-apk/app-debug.apk
```

Or to reinstall (if already installed):

```bash
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

### Method 2: Direct Transfer

1. Copy the APK file to your device:
   ```bash
   adb push build/app/outputs/flutter-apk/app-debug.apk /sdcard/Download/
   ```

2. On your device:
   - Open **Files** or **My Files** app
   - Navigate to **Downloads**
   - Tap the APK file
   - Tap **Install**
   - You may need to allow "Install from unknown sources"

## Step 7: Grant Permissions

When you first open the app:

1. **Usage Stats Access**
   - Tap "Grant Permission"
   - Find "Extra Mind Time" in the list
   - Enable "Permit usage access"
   - Press back to return to the app

2. **Display Over Other Apps**
   - Tap "Grant Permission"
   - Enable "Allow display over other apps"
   - Press back to return to the app

3. **Notifications**
   - Tap "Grant Permission"
   - Allow notifications (required for background service)

## Step 8: Configure the App

1. Tap **Continue** after all permissions are granted
2. Select apps you want to monitor
3. Configure your delay time (1-30 seconds)
4. Set your mindful message
5. Toggle **Monitoring ON**

## Important Notes

### Battery Optimization

For the app to work properly in the background:

1. Go to **Settings** → **Apps** → **Extra Mind Time**
2. Tap **Battery**
3. Select **"Unrestricted"** or **"Don't optimize"**

### Android 13+ (API 33+)

On newer Android versions:
- You may need to grant **Notification permission** separately
- Some manufacturers add extra battery restrictions - check your device's power management settings

### Android 15 Specific

Android 15 works perfectly with this app. Notes:
- All permissions are supported
- Foreground service runs without issues
- Usage Stats API works as expected
- If you have any issues, ensure the app is not in "deep sleep" mode in battery settings

### Manufacturer-Specific Settings

Some manufacturers (Xiaomi, Huawei, Samsung, etc.) have aggressive battery optimization:

**Xiaomi/MIUI:**
- Settings → Apps → Manage apps → Extra Mind Time → Battery saver → No restrictions
- Settings → Apps → Manage apps → Extra Mind Time → Autostart → Enable

**Huawei/EMUI:**
- Settings → Battery → App launch → Extra Mind Time → Manage manually
- Enable all toggles (Auto-launch, Secondary launch, Run in background)

**Samsung:**
- Settings → Apps → Extra Mind Time → Battery → Allow background activity
- Settings → Device care → Battery → Background usage limits → Never sleeping apps → Add Extra Mind Time

**Motorola:**
- Settings → Apps → Extra Mind Time → Battery → Battery optimization → Don't optimize
- Settings → Apps → Extra Mind Time → Battery → Background activity → Allow
- Moto app (if installed) → Battery → Battery optimization → Add Extra Mind Time to exceptions
- Note: Motorola devices typically have cleaner Android and fewer restrictions than other manufacturers

## Troubleshooting

### App not installing

```bash
# Uninstall first
adb uninstall com.example.extra_mind_time

# Then install again
adb install build/app/outputs/flutter-apk/app-debug.apk
```

### Device not detected

```bash
# Restart ADB server
adb kill-server
adb start-server
adb devices
```

### Delay screen not appearing

1. Verify all permissions are granted
2. Check that monitoring is turned ON
3. Disable battery optimization for the app
4. Make sure the foreground notification is visible
5. Restart the app

### App crashes or force closes

View logs:
```bash
adb logcat | grep -i mindful
```

Or view all Flutter logs:
```bash
flutter logs
```

## Uninstalling

### From Device

1. Settings → Apps → Extra Mind Time
2. Tap **Uninstall**

### Using ADB

```bash
adb uninstall com.example.extra_mind_time
```

## Building a Signed Release APK

For distribution, you should sign your APK:

1. Generate a keystore (one time):
   ```bash
   keytool -genkey -v -keystore ~/mindful-time-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mindful-time
   ```

2. Create `android/key.properties`:
   ```
   storePassword=<your-store-password>
   keyPassword=<your-key-password>
   keyAlias=mindful-time
   storeFile=<path-to-your-keystore>
   ```

3. Update `android/app/build.gradle.kts` to use the signing config

4. Build signed APK:
   ```bash
   flutter build apk --release
   ```

## Support

If you encounter issues:
- Check Android version (must be 5.1+)
- Verify all permissions are granted
- Check battery optimization settings
- Review the logs using `adb logcat`

---

**Note**: This app requires special permissions to function properly. It's normal for Android to ask for Usage Access and Display Over Other Apps permissions.