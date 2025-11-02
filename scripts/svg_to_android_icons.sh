#!/bin/bash

# SVG to Android Launcher Icons Converter
# Usage: ./svg_to_android_icons.sh [input_svg_file] [output_directory] [--simplified-adaptive]
#   --simplified-adaptive: Make adaptive icons with transparent background + full logo foreground

set -e

# Default values
DEFAULT_SVG="logo.svg"
DEFAULT_OUTPUT_DIR="android/app/src/main/res"
SIMPLIFIED_ADAPTIVE=false

# Parse arguments
SVG_FILE="$1"
OUTPUT_DIR="$2"
if [ "$3" = "--simplified-adaptive" ]; then
    SIMPLIFIED_ADAPTIVE=true
fi

# Set defaults if not provided
if [ -z "$SVG_FILE" ]; then
    SVG_FILE="$DEFAULT_SVG"
fi
if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR="$DEFAULT_OUTPUT_DIR"
fi

# Check for simplified adaptive flag first
if [ "$1" = "--simplified-adaptive" ]; then
    SIMPLIFIED_ADAPTIVE=true
    SVG_FILE="$DEFAULT_SVG"
    OUTPUT_DIR="$DEFAULT_OUTPUT_DIR"
fi

# Check if SVG file exists
if [ ! -f "$SVG_FILE" ]; then
    echo "Error: SVG file '$SVG_FILE' not found"
    echo "Usage: $0 [svg_file] [output_directory]"
    exit 1
fi

# Check if ImageMagick is available
if ! command -v magick &> /dev/null; then
    echo "Error: ImageMagick 'magick' command not found"
    echo "Please install ImageMagick: brew install imagemagick"
    exit 1
fi

echo "Converting '$SVG_FILE' to Android launcher icons..."
echo "Output directory: $OUTPUT_DIR"

# Create directories if they don't exist
mkdir -p "$OUTPUT_DIR/mipmap-mdpi"
mkdir -p "$OUTPUT_DIR/mipmap-hdpi"
mkdir -p "$OUTPUT_DIR/mipmap-xhdpi"
mkdir -p "$OUTPUT_DIR/mipmap-xxhdpi"
mkdir -p "$OUTPUT_DIR/mipmap-xxxhdpi"
mkdir -p "$OUTPUT_DIR/mipmap-anydpi-v26"

# Generate standard launcher icons
echo "Generating standard launcher icons..."
magick "$SVG_FILE" -resize 48x48 "$OUTPUT_DIR/mipmap-mdpi/ic_launcher.png"
magick "$SVG_FILE" -resize 72x72 "$OUTPUT_DIR/mipmap-hdpi/ic_launcher.png"
magick "$SVG_FILE" -resize 96x96 "$OUTPUT_DIR/mipmap-xhdpi/ic_launcher.png"
magick "$SVG_FILE" -resize 144x144 "$OUTPUT_DIR/mipmap-xxhdpi/ic_launcher.png"
magick "$SVG_FILE" -resize 192x192 "$OUTPUT_DIR/mipmap-xxxhdpi/ic_launcher.png"

# Generate adaptive launcher icons (Android 8.0+)
echo "Generating adaptive launcher icons..."
if [ "$SIMPLIFIED_ADAPTIVE" = true ]; then
    echo "  Using simplified adaptive mode (transparent background + full logo foreground)"
    # Create transparent background
    magick -size 108x108 xc:transparent "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_background.png"
    magick -size 108x108 xc:transparent "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_round_background.png"
    # Full logo as foreground
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_foreground.png"
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_round_foreground.png"
else
    echo "  Using standard adaptive mode (split foreground/background)"
    # Split logo between foreground and background
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_foreground.png"
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_background.png"
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_round_foreground.png"
    magick "$SVG_FILE" -resize 108x108 "$OUTPUT_DIR/mipmap-anydpi-v26/ic_launcher_round_background.png"
fi

# Generate high-res logo for project root
if [ "$OUTPUT_DIR" = "$DEFAULT_OUTPUT_DIR" ]; then
    echo "Generating project logo..."
    magick "$SVG_FILE" -resize 512x512 "logo.png"
fi

echo "✅ All Android launcher icons generated successfully!"
echo ""
echo "Generated files:"
echo "  Standard icons:"
echo "    - mipmap-mdpi/ic_launcher.png (48x48)"
echo "    - mipmap-hdpi/ic_launcher.png (72x72)"
echo "    - mipmap-xhdpi/ic_launcher.png (96x96)"
echo "    - mipmap-xxhdpi/ic_launcher.png (144x144)"
echo "    - mipmap-xxxhdpi/ic_launcher.png (192x192)"
echo "  Adaptive icons (Android 8.0+):"
if [ "$SIMPLIFIED_ADAPTIVE" = true ]; then
    echo "    - mipmap-anydpi-v26/ic_launcher_foreground.png (108x108) - Full logo"
    echo "    - mipmap-anydpi-v26/ic_launcher_background.png (108x108) - Transparent"
    echo "    - mipmap-anydpi-v26/ic_launcher_round_foreground.png (108x108) - Full logo"
    echo "    - mipmap-anydpi-v26/ic_launcher_round_background.png (108x108) - Transparent"
else
    echo "    - mipmap-anydpi-v26/ic_launcher_foreground.png (108x108)"
    echo "    - mipmap-anydpi-v26/ic_launcher_background.png (108x108)"
    echo "    - mipmap-anydpi-v26/ic_launcher_round_foreground.png (108x108)"
    echo "    - mipmap-anydpi-v26/ic_launcher_round_background.png (108x108)"
fi
if [ "$OUTPUT_DIR" = "$DEFAULT_OUTPUT_DIR" ]; then
    echo "  Project logo:"
    echo "    - logo.png (512x512)"
fi
