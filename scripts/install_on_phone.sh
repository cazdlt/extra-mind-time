#!/bin/bash

# Extra Mind Time - Install on Physical Device
# This script builds the release APK and installs it on connected Android device

set -e

echo "📱 Extra Mind Time - Install on Phone"
echo "======================================"
echo ""

# Check if Flutter is installed
if ! command -v flutter &> /dev/null; then
    echo "❌ Flutter is not installed or not in PATH"
    exit 1
fi

# Check for connected devices
echo "🔍 Looking for connected devices..."
DEVICES=$(flutter devices 2>&1)

# Extract device IDs (excluding web, desktop, and emulators)
PHYSICAL_DEVICES=$(echo "$DEVICES" | grep -E "Android [a-zA-Z0-9\s•]+" | grep -v "emulator" | awk '{print $2}')

if [ -z "$PHYSICAL_DEVICES" ]; then
    echo "❌ No physical Android devices found"
    echo ""
    echo "Make sure:"
    echo "  - USB Debugging is enabled in Developer Options"
    echo "  - Phone is connected via USB"
    echo "  - You accepted the USB debugging prompt on your phone"
    echo ""
    echo "All detected devices:"
    echo "$DEVICES"
    exit 1
fi

# Get the first physical device
DEVICE_ID=$(echo "$PHYSICAL_DEVICES" | head -1)

# Get device name
DEVICE_NAME=$(echo "$DEVICES" | grep "$DEVICE_ID" | awk '{for(i=1;i<=NF-1;i++) printf $i" "; print ""}')

echo "✅ Found device: $DEVICE_NAME ($DEVICE_ID)"
echo ""

echo "🔨 Building release APK..."
echo "================================"
flutter build apk --release

echo ""
echo "📦 Installing APK..."
echo "================================"

# Re-check for devices after build
echo "🔍 Checking for connected devices..."
ADB_DEVICES=$(adb devices | grep -v "List of devices" | grep "device$")

if [ -z "$ADB_DEVICES" ]; then
    echo "❌ No connected devices found. Make sure phone is still connected and USB debugging is active."
    exit 1
fi

# Get the first device ID
DEVICE_ID=$(echo "$ADB_DEVICES" | head -1 | awk '{print $1}')
echo "✅ Using device: $DEVICE_ID"

# Install APK using adb
APK_PATH="build/app/outputs/flutter-apk/app-release.apk"

echo "🔄 Uninstalling existing version (if any)..."
adb -s "$DEVICE_ID" uninstall com.example.extra_mind_time 2>/dev/null || true

echo "📦 Installing new version..."
adb -s "$DEVICE_ID" install "$APK_PATH"

echo ""
echo "✅ Installation complete!"
echo "📱 Look for 'Extra Mind Time' in your app drawer"
