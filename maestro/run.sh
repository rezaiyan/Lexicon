#!/usr/bin/env bash
# Maestro test runner — handles install, auth injection, test, and cleanup.
#
# Usage:
#   ./maestro/run.sh                          # run all flows
#   ./maestro/run.sh maestro/flows/study/     # run a category
#   ./maestro/run.sh maestro/flows/study/01_study_screen_loads.yaml  # single flow
#
# Required environment variables (or will be fetched automatically):
#   BACKEND_URL       — Vokab backend URL (defaults to production)
#   CI_TEST_SECRET    — CI auth secret
#
# The script will:
#   1. Install the debug APK
#   2. Launch app (Koin DI init)
#   3. Obtain CI tokens from backend
#   4. Inject tokens via broadcast
#   5. Force-stop app
#   6. Run Maestro tests
#   7. Uninstall app for clean environment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PACKAGE="com.alirezaiyan.vokab"
APK_PATTERN="$PROJECT_DIR/composeApp/build/outputs/apk/debug/*.apk"

# --- Defaults ---
BACKEND_URL="${BACKEND_URL:-https://vokab.alirezaiyan.com}"
CI_TEST_SECRET="${CI_TEST_SECRET:-}"
TEST_TARGET="${1:-maestro/}"

# --- Helpers ---
die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "--- $*"; }

# --- Pre-checks ---
command -v adb     >/dev/null 2>&1 || die "adb not found"
command -v maestro >/dev/null 2>&1 || die "maestro not found"
command -v jq      >/dev/null 2>&1 || die "jq not found"
adb get-state >/dev/null 2>&1     || die "No device/emulator connected"

# Check APK exists
APK=$(ls $APK_PATTERN 2>/dev/null | head -1)
[ -n "$APK" ] || die "Debug APK not found. Run: ./gradlew composeApp:assembleDebug"

# Check CI secret
if [ -z "$CI_TEST_SECRET" ]; then
  # Try reading from docker on VPS (for local dev convenience)
  if command -v ssh >/dev/null 2>&1; then
    CI_TEST_SECRET=$(ssh -o ConnectTimeout=5 root@148.230.109.213 \
      "docker inspect vokab-server --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null" 2>/dev/null \
      | grep '^CI_TEST_SECRET=' | cut -d= -f2) || true
  fi
  [ -n "$CI_TEST_SECRET" ] || die "CI_TEST_SECRET not set. Export it or ensure VPS is reachable."
fi

# --- 1. Install APK ---
info "Installing debug APK"
adb install -r "$APK"

# --- 2. Launch app for Koin init ---
info "Launching app for DI initialization"
adb shell am start -n "$PACKAGE/.MainActivity"
sleep 8

# --- 3. Obtain CI tokens ---
info "Obtaining CI auth tokens from $BACKEND_URL"
HTTP_CODE=$(curl -sL -o /tmp/ci-auth-response.json -w "%{http_code}" \
  --connect-timeout 15 --max-time 30 --retry 3 --retry-delay 5 \
  -X POST "${BACKEND_URL}/api/v1/auth/ci-token" \
  -H "X-CI-Secret: $CI_TEST_SECRET" \
  -H "Content-Type: application/json")

[ "$HTTP_CODE" = "200" ] || die "CI auth endpoint returned HTTP $HTTP_CODE: $(cat /tmp/ci-auth-response.json)"

ACCESS_TOKEN=$(jq -r '.data.accessToken' /tmp/ci-auth-response.json)
REFRESH_TOKEN=$(jq -r '.data.refreshToken' /tmp/ci-auth-response.json)
EXPIRES_IN=$(jq -r '.data.expiresIn' /tmp/ci-auth-response.json)

[ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ] || die "Failed to parse tokens"

# --- 4. Inject tokens via broadcast ---
info "Injecting CI auth tokens"
adb shell "am broadcast \
  -a ${PACKAGE}.CI_INJECT_TOKENS \
  -n ${PACKAGE}/${PACKAGE}.CiTokenReceiver \
  --es accessToken '${ACCESS_TOKEN}' \
  --es refreshToken '${REFRESH_TOKEN}' \
  --el expiresInMs ${EXPIRES_IN}"
sleep 2

# --- 5. Force-stop app ---
info "Force-stopping app"
adb shell am force-stop "$PACKAGE"
sleep 2

# --- 6. Run Maestro tests ---
info "Running Maestro tests: $TEST_TARGET"
# Generate a unique test word for each run so new words are always due for review.
TEST_WORD="Test$(date +%s)"
MAESTRO_EXIT=0
maestro test --env TEST_WORD="$TEST_WORD" "$TEST_TARGET" || MAESTRO_EXIT=$?

# --- 7. Uninstall app ---
info "Uninstalling app for clean environment"
adb uninstall "$PACKAGE" || true

if [ "$MAESTRO_EXIT" -eq 0 ]; then
  echo "All Maestro tests passed."
else
  echo "Maestro tests failed (exit code $MAESTRO_EXIT)."
fi
exit $MAESTRO_EXIT
