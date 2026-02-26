#!/usr/bin/env bash
set -euo pipefail

SHERPA_VERSION="1.12.26"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LIBS_DIR="$PROJECT_ROOT/platforms/libs"

SHERPA_AAR_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/sherpa-onnx-${SHERPA_VERSION}.aar"
SHERPA_IOS_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/sherpa-onnx-v${SHERPA_VERSION}-ios.tar.bz2"

check_binary_exists() {
    local path="$1"
    if [[ -f "$path" ]]; then
        return 0
    fi
    return 1
}

already_downloaded() {
    check_binary_exists "$LIBS_DIR/sherpa-onnx-${SHERPA_VERSION}.aar" &&
    check_binary_exists "$LIBS_DIR/build-ios/sherpa-onnx.xcframework/ios-arm64/libsherpa-onnx.a" &&
    check_binary_exists "$LIBS_DIR/build-ios/ios-onnxruntime/1.17.1/onnxruntime.xcframework/ios-arm64/onnxruntime.a"
}

if already_downloaded; then
    echo "TTS libraries already present, skipping download."
    exit 0
fi

echo "Downloading TTS native libraries (sherpa-onnx v${SHERPA_VERSION})..."

# Download Android AAR
echo "  Downloading Android AAR..."
curl -fSL --progress-bar -o "$LIBS_DIR/sherpa-onnx-${SHERPA_VERSION}.aar" "$SHERPA_AAR_URL"

# Download and extract iOS frameworks
echo "  Downloading iOS xcframeworks..."
curl -fSL --progress-bar "$SHERPA_IOS_URL" | tar xjf - -C "$LIBS_DIR"

echo "Done. TTS libraries installed to platforms/libs/"
