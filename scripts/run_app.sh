#!/bin/bash

# Extra Mind Time - Quick Start Script
# This script launches the Android emulator and runs the Flutter app

set -e

echo "🚀 Extra Mind Time - Quick Start"
echo "================================"
echo ""

# Check if Flutter is installed
if ! command -v flutter &> /dev/null; then
    echo "❌ Flutter is not installed or not in PATH"
    exit 1
fi

# Check for available emulators
echo "📱 Checking for Android emulators..."
EMULATOR_LIST=$(flutter emulators 2>&1)

if echo "$EMULATOR_LIST" | grep -q "No emulators available"; then
    echo "❌ No Android emulators found"
    echo "Create one with: flutter emulators --create"
    exit 1
fi

echo "$EMULATOR_LIST"
echo ""

# Get the first emulator ID (match lines with bullet point separator)
EMULATOR_ID=$(flutter emulators 2>&1 | awk '/^[a-z][a-z0-9_]*[ \t]+•/ {print $1}' | head -1)

if [ -z "$EMULATOR_ID" ]; then
    echo "❌ Could not detect emulator ID"
    exit 1
fi

echo "🎯 Using emulator: $EMULATOR_ID"
echo ""

# Check if emulator is already running
if adb devices | grep -q "emulator"; then
    echo "✅ Emulator is already running"
else
    echo "🔄 Launching emulator..."
    flutter emulators --launch "$EMULATOR_ID" &

    # Wait for emulator to be ready
    echo "⏳ Waiting for emulator to boot (this may take 30-60 seconds)..."
    adb wait-for-device
    sleep 10

    # Wait for boot to complete
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
        echo "   Still booting..."
        sleep 5
    done

    echo "✅ Emulator is ready!"
fi

echo ""
echo "🏃 Running Flutter app..."
echo "================================"
echo ""
echo "💡 Tips:"
echo "  - Press 'r' for hot reload"
echo "  - Press 'R' for hot restart"
echo "  - Press 'q' to quit"
echo ""

# Run the app
flutter run
