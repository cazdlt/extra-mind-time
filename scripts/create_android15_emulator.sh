
# Create Android 15 Emulator Script
# This script creates and configures an Android 15 emulator with recommended settings

set -e

echo "📱 Creating Android 15 Emulator"
echo "==============================="
echo ""

# Default configuration
DEVICE_NAME="pixel_8"
AVD_NAME="android15_pixel8"
PACKAGE="system-images;android-35;google_apis_playstore;arm64-v8a"
DEVICE_ID="pixel_8"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --name)
            AVD_NAME="$2"
            shift 2
            ;;
        --device)
            DEVICE_ID="$2"
            shift 2
            ;;
        --package)
            PACKAGE="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --name NAME     AVD name (default: android15_pixel8)"
            echo "  --device ID    Device type (default: pixel_8)"
            echo "  --package PKG  System image package"
            echo "  --help, -h      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if required tools are available
if ! command -v sdkmanager &> /dev/null; then
    echo "❌ sdkmanager is not installed or not in PATH"
    echo "Please install Android SDK command-line tools"
    exit 1
fi

if ! command -v avdmanager &> /dev/null; then
    echo "❌ avdmanager is not installed or not in PATH"
    echo "Please install Android SDK command-line tools"
    exit 1
fi

# Check if emulator already exists
if avdmanager list avd | grep -q "$AVD_NAME"; then
    echo "⚠️  Emulator '$AVD_NAME' already exists"
    echo "Delete it first with: avdmanager delete avd --name $AVD_NAME"
    exit 1
fi

echo "🔧 Installing Android 15 system image..."
echo "Package: $PACKAGE"
echo ""

# Install the required system image
sdkmanager "$PACKAGE"

echo ""
echo "📋 Available device definitions:"
avdmanager list devices | grep -E "id:|Name:" | head -20

echo ""
echo "🎯 Creating AVD: $AVD_NAME"
echo "Device: $DEVICE_ID"
echo "System Image: $PACKAGE"
echo ""

# Create the AVD with hardware acceleration and recommended settings
echo "no" | avdmanager create avd \
    --name "$AVD_NAME" \
    --device "$DEVICE_ID" \
    --package "$PACKAGE" \
    --force

# Create config.ini with optimal settings for performance
CONFIG_PATH="$HOME/.android/avd/$AVD_NAME.avd/config.ini"

echo "⚙️  Optimizing emulator configuration..."

cat >> "$CONFIG_PATH" << 'EOF'

# Performance optimizations
hw.gpu.enabled = yes
hw.gpu.mode = host
hw.lcd.density = 440
hw.ramSize = 4096
vm.heapSize = 512
hw.initialOrientation = portrait

# Input settings
hw.keyboard = yes
hw.mainKeys = no

# Network settings
hw.camera.back = emulated
hw.camera.front = emulated

# System settings
hw.audioInput = yes
hw.audioOutput = yes

# Display settings
hw.screen.height = 2400
hw.screen.width = 1080

# Boot settings
showDeviceFrame = yes
avd.ini.displayname = Android 15 Pixel 8
EOF

echo ""
echo "✅ Android 15 emulator created successfully!"
echo ""
echo "📋 Emulator Details:"
echo "  Name: $AVD_NAME"
echo "  Device: $DEVICE_ID"
echo "  API Level: 35 (Android 15)"
echo "  Architecture: ARM 64-bit"
echo "  Google Play Store: Enabled"
echo ""
echo "🚀 To launch the emulator:"
echo "  emulator -avd $AVD_NAME"
echo ""
echo "🔧 To use with Flutter:"
echo "  flutter emulators --launch $AVD_NAME"
echo ""
echo "💡 Tips:"
echo "  - First boot may take 2-3 minutes"
echo "  - Use hardware acceleration for better performance"
echo "  - Allocate at least 4GB RAM for smooth operation"
