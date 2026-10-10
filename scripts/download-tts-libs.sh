#!/usr/bin/env bash
set -euo pipefail

# 1.13.5+ fixes Piper phoneme framing (k2-fsa/sherpa-onnx#3721) that skewed pronunciation.
SHERPA_VERSION="1.13.8"
ONNXRUNTIME_IOS_VERSION="1.28.2"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LIBS_DIR="$PROJECT_ROOT/platforms/libs"

SHERPA_AAR_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/sherpa-onnx-${SHERPA_VERSION}.aar"

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
    check_binary_exists "$LIBS_DIR/build-ios/ios-onnxruntime/${ONNXRUNTIME_IOS_VERSION}/onnxruntime.xcframework/ios-arm64/onnxruntime.a"
}

if already_downloaded; then
    echo "TTS libraries already present, skipping download."
    exit 0
fi

echo "Downloading TTS native libraries (sherpa-onnx v${SHERPA_VERSION})..."

# Download Android AAR
echo "  Downloading Android AAR..."
curl -fSL --progress-bar -o "$LIBS_DIR/sherpa-onnx-${SHERPA_VERSION}.aar" "$SHERPA_AAR_URL"

# Upstream stopped publishing prebuilt iOS archives after 1.13.4, so build from source.
# Set SHERPA_BUILD_DIR to an existing sherpa-onnx/build-ios dir to skip the clone + build.
if [[ -z "${SHERPA_BUILD_DIR:-}" ]]; then
    echo "  Building iOS libraries from source (takes a while)..."
    WORK_DIR="$(mktemp -d)"
    trap 'rm -rf "$WORK_DIR"' EXIT
    git clone -q --depth 1 --branch "v${SHERPA_VERSION}" https://github.com/k2-fsa/sherpa-onnx.git "$WORK_DIR/sherpa-onnx"
    (cd "$WORK_DIR/sherpa-onnx" && SHERPA_ONNX_ONNXRUNTIME_VERSION="$ONNXRUNTIME_IOS_VERSION" ./build-ios.sh)
    SHERPA_BUILD_DIR="$WORK_DIR/sherpa-onnx/build-ios"
fi

# Upstream packs framework bundles; platforms/build.gradle.kts links plain <arch>/lib*.a + Headers.
IOS_DIR="$LIBS_DIR/build-ios"
SHERPA_OUT="$IOS_DIR/sherpa-onnx.xcframework"
ORT_SRC="$SHERPA_BUILD_DIR/ios-onnxruntime/${ONNXRUNTIME_IOS_VERSION}/onnxruntime.xcframework"
ORT_OUT="$IOS_DIR/ios-onnxruntime/${ONNXRUNTIME_IOS_VERSION}/onnxruntime.xcframework"
rm -rf "$SHERPA_OUT" "$IOS_DIR/ios-onnxruntime"

for pair in "ios-arm64:os64" "ios-arm64_x86_64-simulator:simulator"; do
    arch="${pair%%:*}"
    build="${pair##*:}"
    mkdir -p "$SHERPA_OUT/$arch/Headers/sherpa-onnx/c-api" "$ORT_OUT/$arch"
    cp "$SHERPA_BUILD_DIR/build/$build/libsherpa-onnx-c-api.a" "$SHERPA_OUT/$arch/libsherpa-onnx.a"
    cp "$SHERPA_BUILD_DIR/install/include/sherpa-onnx/c-api/c-api.h" "$SHERPA_OUT/$arch/Headers/sherpa-onnx/c-api/"
    cp "$ORT_SRC/$arch/onnxruntime.framework/onnxruntime" "$ORT_OUT/$arch/onnxruntime.a"
    ln -sf onnxruntime.a "$ORT_OUT/$arch/libonnxruntime.a"
done
ln -sfn "${ONNXRUNTIME_IOS_VERSION}/onnxruntime.xcframework" "$IOS_DIR/ios-onnxruntime/onnxruntime.xcframework"

echo "Done. TTS libraries installed to platforms/libs/"
