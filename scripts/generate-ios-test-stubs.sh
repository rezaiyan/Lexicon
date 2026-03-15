#!/usr/bin/env bash
# Generates minimal static stub frameworks for iOS test linking.
#
# Third-party iOS SDKs (GoogleSignIn, Facebook SDK, Firebase, RevenueCat)
# are resolved via SPM in the Xcode project but are unavailable when Gradle
# links the Kotlin/Native iOS test binary directly.  These empty static
# stubs satisfy the linker so commonTest can run on iosSimulatorArm64.
# Using static libraries avoids runtime dyld lookup failures.
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
    FBAEMKit
    FBSDKCoreKit_Basics
    FirebaseAuth
    FirebaseAuthInterop
    FirebaseCore
    PurchasesHybridCommon
    RevenueCat
)

SDK_PATH="$(xcrun --sdk iphonesimulator --show-sdk-path)"

for fw in "${FRAMEWORKS[@]}"; do
    FW_DIR="$STUB_DIR/$fw.framework"
    BINARY="$FW_DIR/$fw"

    # Skip if stub already exists and is valid
    if [ -f "$BINARY" ] && file "$BINARY" | grep -q "archive"; then
        continue
    fi

    mkdir -p "$FW_DIR"

    SRC_FILE="$FW_DIR/${fw}_stub.c"
    OBJ_FILE="$FW_DIR/${fw}_stub.o"

    # FBSDKCoreKit requires specific FBLinkable_* symbols that the cinterop
    # cache references at startup.  Provide empty definitions so the test
    # binary doesn't crash at runtime.
    if [ "$fw" = "FBSDKCoreKit" ]; then
        cat > "$SRC_FILE" <<'CSRC'
// Stub symbols expected by FBSDKCoreKit cinterop cache
void FBLinkable_NSBundle_InfoDictionaryProviding(void) {}
void FBLinkable_NSData_FileDataExtracting(void) {}
void FBLinkable_NSFileManager_FileManaging(void) {}
void FBLinkable_NSNotificationCenter_NotificationDelivering(void) {}
void FBLinkable_NSNotificationCenter_NotificationPosting(void) {}
void FBLinkable_NSProcessInfo_MacCatalystDetermining(void) {}
void FBLinkable_NSProcessInfo_OperatingSystemVersionComparing(void) {}
void FBLinkable_NSURLSessionTask_NetworkTask(void) {}
void FBLinkable_NSURLSession_URLSessionProviding(void) {}
void FBLinkable_NSUserDefaults_DataPersisting(void) {}
void FBLinkable_UIPasteboard_FBSDKPasteboard(void) {}
CSRC
    else
        echo "// empty stub" > "$SRC_FILE"
    fi

    xcrun clang \
        -target arm64-apple-ios15.0-simulator \
        -c \
        -o "$OBJ_FILE" \
        "$SRC_FILE" \
        -isysroot "$SDK_PATH" \
        2>/dev/null

    xcrun libtool -static -o "$BINARY" "$OBJ_FILE" 2>/dev/null
    rm -f "$OBJ_FILE" "$SRC_FILE"

    cat > "$FW_DIR/module.modulemap" <<MODULEMAP
framework module $fw {
}
MODULEMAP

    echo "Created stub: $fw.framework (static)"
done

echo "Stub frameworks ready at: $STUB_DIR"
