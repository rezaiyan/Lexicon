#!/usr/bin/env bash
# Generates minimal stub frameworks for iOS test linking.
#
# Third-party iOS SDKs (GoogleSignIn, Facebook SDK, Firebase, RevenueCat)
# are resolved via SPM in the Xcode project but are unavailable when Gradle
# links the Kotlin/Native iOS test binary directly.  These empty stubs
# satisfy the linker so commonTest can run on iosSimulatorArm64.
#
# Usage: ./scripts/generate-ios-test-stubs.sh [output-dir]
#   Default output: composeApp/build/ios-test-stub-frameworks

set -euo pipefail

STUB_DIR="${1:-$(dirname "$0")/../composeApp/build/ios-test-stub-frameworks}"
STUB_DIR="$(cd "$(dirname "$STUB_DIR")" && pwd)/$(basename "$STUB_DIR")"

FRAMEWORKS=(
    GoogleSignIn
    FBSDKCoreKit
    FBSDKLoginKit
    FirebaseAuth
    FirebaseCore
    PurchasesHybridCommon
)

SDK_PATH="$(xcrun --sdk iphonesimulator --show-sdk-path)"

for fw in "${FRAMEWORKS[@]}"; do
    FW_DIR="$STUB_DIR/$fw.framework"
    BINARY="$FW_DIR/$fw"

    # Skip if stub already exists and is valid
    if [ -f "$BINARY" ] && file "$BINARY" | grep -q "Mach-O"; then
        continue
    fi

    mkdir -p "$FW_DIR"

    # Create a minimal dynamic library stub (empty translation unit)
    echo "" | xcrun clang \
        -target arm64-apple-ios15.0-simulator \
        -dynamiclib \
        -install_name "@rpath/$fw.framework/$fw" \
        -o "$BINARY" \
        -x c - \
        -isysroot "$SDK_PATH" \
        2>/dev/null

    echo "Created stub: $fw.framework"
done

echo "Stub frameworks ready at: $STUB_DIR"
