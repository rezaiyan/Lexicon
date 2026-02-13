#!/bin/bash
# Build and deploy iOS app to connected device
#
# Usage: ./scripts/deploy-ios-device.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "$PROJECT_ROOT"

echo "▸ Building iOS framework for device (arm64)..."
./gradlew composeApp:linkDebugFrameworkIosArm64

echo "▸ Building and deploying to device via Xcode..."
cd iosApp

# Get the first connected device UDID
DEVICE_UDID=$(xcrun xctrace list devices 2>&1 | grep "Ali's iPhone" | grep -v "Offline" | head -1 | sed -E 's/.*\(([A-F0-9-]+)\)/\1/')

if [[ -z "$DEVICE_UDID" ]]; then
    echo "✗ No iPhone connected. Please connect Ali's iPhone and try again."
    exit 1
fi

echo "▸ Found device: $DEVICE_UDID"

# Build and install to device
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination "id=$DEVICE_UDID" \
    -allowProvisioningUpdates \
    build install

echo "✓ App deployed to iPhone!"
echo "  If you see 'Untrusted Developer', go to:"
echo "  Settings → General → VPN & Device Management → Trust"
