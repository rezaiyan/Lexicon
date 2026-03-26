#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────
# record_showcase.sh — Record Maestro showcase flows as MP4/GIF demos.
#
# Handles the full lifecycle: build APK, uninstall old app, fresh install,
# auth token injection, record each flow, convert to GIF.
#
# Usage:
#   ./maestro/record_showcase.sh                     # record all showcase flows
#   ./maestro/record_showcase.sh study_dashboard      # record a single flow
#   ./maestro/record_showcase.sh --gif                # convert existing MP4s to GIFs
#   ./maestro/record_showcase.sh --skip-build         # skip APK build (use existing)
#
# Required environment variables (or fetched automatically):
#   BACKEND_URL       — Vokab backend URL (defaults to production)
#   CI_TEST_SECRET    — CI auth secret (fetched from VPS if not set)
#
# Output:
#   docs/demos/*.mp4  — raw screen recordings
#   docs/demos/*.gif  — optimized GIF animations (for README)
# ──────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SHOWCASE_DIR="$SCRIPT_DIR/flows/showcase"
OUTPUT_DIR="$PROJECT_DIR/docs/demos"

PACKAGE="com.alirezaiyan.vokab"
APK_PATTERN="$PROJECT_DIR/composeApp/build/outputs/apk/debug/*.apk"

BACKEND_URL="${BACKEND_URL:-https://vokab.alirezaiyan.com}"
CI_TEST_SECRET="${CI_TEST_SECRET:-}"

# Showcase flows — onboarding FIRST (needs clearState), then authenticated flows.
FLOWS=(
  "onboarding_showcase"
  "study_dashboard_showcase"
  "import_words_showcase"
  "flashcard_review_showcase"
  "ai_import_showcase"
  "word_manager_showcase"
  "profile_settings_showcase"
)

# GIF settings.
GIF_WIDTH=360          # Width in pixels (height auto-calculated).
GIF_FPS=15             # Frames per second.

# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────

info()  { printf "\033[1;34m▸ %s\033[0m\n" "$*"; }
ok()    { printf "\033[1;32m✓ %s\033[0m\n" "$*"; }
warn()  { printf "\033[1;33m⚠ %s\033[0m\n" "$*"; }
fail()  { printf "\033[1;31m✗ %s\033[0m\n" "$*"; exit 1; }

check_prereqs() {
  command -v adb      >/dev/null 2>&1 || fail "adb not found. Install Android SDK platform-tools."
  command -v maestro  >/dev/null 2>&1 || fail "maestro not found. Install: curl -fsSL https://get.maestro.mobile.dev | bash"
  command -v jq       >/dev/null 2>&1 || fail "jq not found. Install: brew install jq"

  local devices
  devices=$(adb devices | grep -c -E 'device$') || true
  [ "$devices" -ge 1 ] || fail "No Android device/emulator found. Start one first."
}

ensure_output_dir() {
  mkdir -p "$OUTPUT_DIR"
}

# ──────────────────────────────────────────────
# App lifecycle: build, uninstall, install, auth
# ──────────────────────────────────────────────

build_apk() {
  info "Building debug APK..."
  "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" composeApp:assembleDebug
  ok "APK built"
}

uninstall_app() {
  info "Uninstalling app (clean slate)"
  adb uninstall "$PACKAGE" 2>/dev/null || true
  ok "App uninstalled"
}

install_app() {
  local apk
  apk=$(ls $APK_PATTERN 2>/dev/null | head -1)
  [ -n "$apk" ] || fail "Debug APK not found. Run: ./gradlew composeApp:assembleDebug"

  info "Installing fresh debug APK: $(basename "$apk")"
  adb install -r "$apk"
  ok "APK installed"
}

fetch_ci_secret() {
  if [ -n "$CI_TEST_SECRET" ]; then
    return
  fi

  info "CI_TEST_SECRET not set — fetching from VPS..."
  if command -v ssh >/dev/null 2>&1; then
    CI_TEST_SECRET=$(ssh -o ConnectTimeout=5 root@alirezaiyan.com \
      "docker inspect vokab-server --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null" 2>/dev/null \
      | grep '^CI_TEST_SECRET=' | cut -d= -f2) || true
  fi
  [ -n "$CI_TEST_SECRET" ] || fail "CI_TEST_SECRET not set. Export it or ensure VPS is reachable."
  ok "CI secret obtained"
}

inject_auth_tokens() {
  info "Launching app for DI initialization..."
  adb shell am start -n "$PACKAGE/.MainActivity"
  sleep 8

  info "Obtaining CI auth tokens from $BACKEND_URL"
  local http_code
  http_code=$(curl -sL -o /tmp/ci-auth-response.json -w "%{http_code}" \
    --connect-timeout 15 --max-time 30 --retry 3 --retry-delay 5 \
    -X POST "${BACKEND_URL}/api/v1/auth/ci-token" \
    -H "X-CI-Secret: $CI_TEST_SECRET" \
    -H "Content-Type: application/json")

  [ "$http_code" = "200" ] || fail "CI auth endpoint returned HTTP $http_code: $(cat /tmp/ci-auth-response.json)"

  local access_token refresh_token expires_in
  access_token=$(jq -r '.data.accessToken' /tmp/ci-auth-response.json)
  refresh_token=$(jq -r '.data.refreshToken' /tmp/ci-auth-response.json)
  expires_in=$(jq -r '.data.expiresIn' /tmp/ci-auth-response.json)

  [ "$access_token" != "null" ] && [ -n "$access_token" ] || fail "Failed to parse tokens"

  info "Injecting CI auth tokens"
  adb shell "am broadcast \
    -a ${PACKAGE}.CI_INJECT_TOKENS \
    -n ${PACKAGE}/${PACKAGE}.CiTokenReceiver \
    --es accessToken '${access_token}' \
    --es refreshToken '${refresh_token}' \
    --el expiresInMs ${expires_in}"
  sleep 2

  adb shell am force-stop "$PACKAGE"
  sleep 2

  ok "Auth tokens injected"
}

# ──────────────────────────────────────────────
# Record a single showcase flow
# ──────────────────────────────────────────────

record_flow() {
  local flow_name="$1"
  local yaml="$SHOWCASE_DIR/${flow_name}.yaml"
  local mp4="$OUTPUT_DIR/${flow_name}.mp4"
  local device_path="/sdcard/${flow_name}.mp4"

  [ -f "$yaml" ] || fail "Flow not found: $yaml"

  info "Recording: $flow_name"

  # Clean up any previous recording on device.
  adb shell rm -f "$device_path" 2>/dev/null || true

  # Start screen recording in the background.
  adb shell screenrecord --size 1080x1920 --bit-rate 8000000 "$device_path" &
  local record_pid=$!

  # Give screenrecord a moment to start.
  sleep 1

  # Run the Maestro flow.
  if maestro test "$yaml"; then
    ok "Flow completed: $flow_name"
  else
    warn "Flow had issues: $flow_name (recording may still be usable)"
  fi

  # Stop screen recording.
  sleep 1
  adb shell pkill -INT screenrecord 2>/dev/null || true
  wait "$record_pid" 2>/dev/null || true

  # Wait for the file to be written.
  sleep 2

  # Pull the recording.
  if adb pull "$device_path" "$mp4" 2>/dev/null; then
    ok "Saved: $mp4"
    adb shell rm -f "$device_path" 2>/dev/null || true
  else
    warn "Failed to pull recording for: $flow_name"
  fi
}

# ──────────────────────────────────────────────
# Convert MP4 to optimized GIF
# ──────────────────────────────────────────────

convert_to_gif() {
  local flow_name="$1"
  local mp4="$OUTPUT_DIR/${flow_name}.mp4"
  local gif="$OUTPUT_DIR/${flow_name}.gif"

  [ -f "$mp4" ] || { warn "MP4 not found, skipping: $mp4"; return; }

  command -v ffmpeg >/dev/null 2>&1 || fail "ffmpeg not found. Install: brew install ffmpeg"

  info "Converting to GIF: $flow_name"

  # Two-pass GIF for optimal quality:
  # 1. Generate palette from the video.
  # 2. Use palette for high-quality GIF encoding.
  local palette="/tmp/${flow_name}_palette.png"

  ffmpeg -y -i "$mp4" \
    -vf "fps=${GIF_FPS},scale=${GIF_WIDTH}:-1:flags=lanczos,palettegen=stats_mode=diff" \
    "$palette" 2>/dev/null

  ffmpeg -y -i "$mp4" -i "$palette" \
    -lavfi "fps=${GIF_FPS},scale=${GIF_WIDTH}:-1:flags=lanczos [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
    "$gif" 2>/dev/null

  rm -f "$palette"

  local size
  size=$(du -h "$gif" | cut -f1)
  ok "Created GIF ($size): $gif"
}

# ──────────────────────────────────────────────
# Full setup: build, uninstall, install, auth
# ──────────────────────────────────────────────

setup_fresh_app() {
  local skip_build="$1"

  if [ "$skip_build" = "false" ]; then
    build_apk
  else
    info "Skipping APK build (--skip-build)"
  fi

  uninstall_app
  install_app
  fetch_ci_secret
}

# After onboarding_showcase clears state, we need to re-inject auth
# and reinstall so subsequent flows have a logged-in user.
reinstall_after_onboarding() {
  info "Reinstalling app after onboarding recording (restoring auth)..."
  uninstall_app
  install_app
  inject_auth_tokens
  ok "App reinstalled and authenticated — ready for remaining flows"
}

# ──────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────

main() {
  local mode="record"  # record | gif | single
  local single_flow=""
  local skip_build="false"

  # Parse arguments.
  for arg in "$@"; do
    case "$arg" in
      --gif)
        mode="gif"
        ;;
      --skip-build)
        skip_build="true"
        ;;
      --help|-h)
        echo "Usage: $0 [flow_name] [--gif] [--skip-build]"
        echo ""
        echo "  flow_name      Record a single flow (e.g., study_dashboard)"
        echo "  --gif          Convert existing MP4 recordings to GIFs (no device needed)"
        echo "  --skip-build   Skip APK build, use existing debug APK"
        echo ""
        echo "The script will:"
        echo "  1. Build the debug APK (unless --skip-build)"
        echo "  2. Uninstall any existing app"
        echo "  3. Fresh-install the APK"
        echo "  4. Record onboarding (clears state)"
        echo "  5. Reinstall + inject auth tokens"
        echo "  6. Record all authenticated flows"
        echo "  7. Convert recordings to GIFs"
        echo "  8. Uninstall app (clean up)"
        echo ""
        echo "Available flows:"
        for f in "${FLOWS[@]}"; do
          echo "  - $f"
        done
        exit 0
        ;;
      *)
        # Append _showcase if not already present.
        if [[ "$arg" == *_showcase ]]; then
          single_flow="$arg"
        else
          single_flow="${arg}_showcase"
        fi
        mode="single"
        ;;
    esac
  done

  ensure_output_dir

  case "$mode" in
    gif)
      info "Converting all MP4 recordings to GIFs..."
      for flow in "${FLOWS[@]}"; do
        convert_to_gif "$flow"
      done
      ok "All GIF conversions complete!"
      echo ""
      echo "GIFs are in: $OUTPUT_DIR/"
      ;;

    single)
      check_prereqs
      fetch_ci_secret

      local needs_onboarding="false"
      if [ "$single_flow" = "onboarding_showcase" ]; then
        needs_onboarding="true"
      fi

      # Fresh install for a clean recording.
      setup_fresh_app "$skip_build"

      if [ "$needs_onboarding" = "true" ]; then
        # Onboarding uses clearState: true — no auth needed before recording.
        record_flow "$single_flow"
      else
        # Authenticated flow — inject tokens first.
        inject_auth_tokens
        record_flow "$single_flow"
      fi

      convert_to_gif "$single_flow"

      # Clean up.
      uninstall_app
      ok "Done! Recording: $OUTPUT_DIR/${single_flow}.mp4"
      ;;

    record)
      check_prereqs
      fetch_ci_secret
      info "Recording all ${#FLOWS[@]} showcase flows..."
      echo ""

      # ── Phase 1: Fresh install ──
      setup_fresh_app "$skip_build"

      # ── Phase 2: Record onboarding (clearState, no auth needed) ──
      info "Phase 1/3: Recording onboarding (fresh app, no auth)"
      record_flow "onboarding_showcase"
      echo ""

      # ── Phase 3: Reinstall + auth for remaining flows ──
      info "Phase 2/3: Reinstalling app and injecting auth tokens"
      reinstall_after_onboarding
      echo ""

      # ── Phase 4: Record all authenticated flows ──
      info "Phase 3/3: Recording authenticated flows"
      for flow in "${FLOWS[@]}"; do
        if [ "$flow" = "onboarding_showcase" ]; then
          continue  # Already recorded.
        fi
        record_flow "$flow"
        echo ""
      done

      # ── Phase 5: Convert to GIF ──
      info "Converting all recordings to GIF..."
      echo ""
      for flow in "${FLOWS[@]}"; do
        convert_to_gif "$flow"
      done

      # ── Phase 6: Clean up ──
      uninstall_app

      echo ""
      ok "All recordings complete!"
      echo ""
      echo "Output directory: $OUTPUT_DIR/"
      echo ""
      echo "Files:"
      ls -lh "$OUTPUT_DIR/"*.mp4 2>/dev/null || true
      ls -lh "$OUTPUT_DIR/"*.gif 2>/dev/null || true
      ;;
  esac
}

main "$@"
