#!/bin/bash

# Stop All Android Emulators Script
# This script stops all currently running Android emulators

set -e

echo "🛑 Stopping All Android Emulators"
echo "=================================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ adb is not installed or not in PATH"
    exit 1
fi

# Get list of running emulators
EMULATORS=$(adb devices | grep "emulator" | awk '{print $1}')

if [ -z "$EMULATORS" ]; then
    echo "✅ No emulators are currently running"
    exit 0
fi

echo "Found running emulators:"
echo "$EMULATORS"
echo ""

# Stop each emulator
while IFS= read -r emulator; do
    if [ -n "$emulator" ]; then
        echo "⏹️  Stopping $emulator..."
        adb -s "$emulator" emu kill
    fi
done <<< "$EMULATORS"

echo ""
echo "⏳ Waiting for emulators to shut down..."
sleep 2

# Verify all emulators are stopped
REMAINING=$(adb devices | grep "emulator" | awk '{print $1}')

if [ -z "$REMAINING" ]; then
    echo "✅ All emulators stopped successfully!"
else
    echo "⚠️  Some emulators may still be running:"
    echo "$REMAINING"
    echo ""
    echo "You can manually kill them with: pkill -9 qemu-system"
fi
