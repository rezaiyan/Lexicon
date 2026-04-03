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

# --- Phase planning ---
# Phase 1 (premium auth): run main suite — all flows except premium_gates/
# Phase 2 (non-premium auth): run flows/premium_gates/ to verify gating behaviour
#
# Phase 2 runs when:
#   a) Full suite: TEST_TARGET == "maestro/"
#   b) Targeted at premium_gates: TEST_TARGET contains "premium_gates"
# When targeting premium_gates directly, Phase 1 is skipped (no point running
# premium flows with a non-premium token injection).
if [[ "$TEST_TARGET" == "maestro/" ]]; then
  RUN_MAIN=true
  RUN_PREMIUM_GATES=true
elif [[ "$TEST_TARGET" == *"premium_gates"* ]]; then
  RUN_MAIN=false
  RUN_PREMIUM_GATES=true
else
  RUN_MAIN=true
  RUN_PREMIUM_GATES=false
fi

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
    CI_TEST_SECRET=$(ssh -o ConnectTimeout=5 root@alirezaiyan.com \
      "docker exec lexicon-server env 2>/dev/null" 2>/dev/null \
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

# Generate a unique test word for each run so new words are always due for review.
TEST_WORD="Test$(date +%s)"
MAESTRO_EXIT=0
PREMIUM_GATES_EXIT=0

# --- 6. Run Maestro tests (Phase 1 — premium user) ---
if [ "$RUN_MAIN" = true ]; then
  info "Running Maestro tests (Phase 1 — premium): $TEST_TARGET"
  maestro test --env TEST_WORD="$TEST_WORD" "$TEST_TARGET" || MAESTRO_EXIT=$?
fi

# --- Phase 2 — non-premium user: verify premium gates ---
if [ "$RUN_PREMIUM_GATES" = true ]; then
  info "Phase 2: injecting non-premium tokens for premium-gate tests"

  HTTP_CODE=$(curl -sL -o /tmp/ci-auth-response-np.json -w "%{http_code}" \
    --connect-timeout 15 --max-time 30 --retry 3 --retry-delay 5 \
    -X POST "${BACKEND_URL}/api/v1/auth/ci-token?premium=false" \
    -H "X-CI-Secret: $CI_TEST_SECRET" \
    -H "Content-Type: application/json")

  [ "$HTTP_CODE" = "200" ] || { info "WARNING: non-premium CI token returned HTTP $HTTP_CODE — skipping Phase 2"; PREMIUM_GATES_EXIT=1; }

  if [ "$HTTP_CODE" = "200" ]; then
    NP_ACCESS_TOKEN=$(jq -r '.data.accessToken' /tmp/ci-auth-response-np.json)
    NP_REFRESH_TOKEN=$(jq -r '.data.refreshToken' /tmp/ci-auth-response-np.json)
    NP_EXPIRES_IN=$(jq -r '.data.expiresIn' /tmp/ci-auth-response-np.json)

    [ "$NP_ACCESS_TOKEN" != "null" ] && [ -n "$NP_ACCESS_TOKEN" ] || \
      { info "WARNING: failed to parse non-premium tokens — skipping Phase 2"; PREMIUM_GATES_EXIT=1; }
  fi

  if [ "${PREMIUM_GATES_EXIT:-0}" -eq 0 ]; then
    info "Injecting non-premium CI auth tokens"
    adb shell "am broadcast \
      -a ${PACKAGE}.CI_INJECT_TOKENS \
      -n ${PACKAGE}/${PACKAGE}.CiTokenReceiver \
      --es accessToken '${NP_ACCESS_TOKEN}' \
      --es refreshToken '${NP_REFRESH_TOKEN}' \
      --el expiresInMs ${NP_EXPIRES_IN}"
    sleep 2

    info "Force-stopping app before premium-gate tests"
    adb shell am force-stop "$PACKAGE"
    sleep 2

    info "Running Maestro tests (Phase 2 — non-premium premium gates)"
    maestro test "$SCRIPT_DIR/flows/premium_gates/" || PREMIUM_GATES_EXIT=$?
  fi
fi

# --- 7. Uninstall app ---
info "Uninstalling app for clean environment"
adb uninstall "$PACKAGE" || true

COMBINED_EXIT=$(( MAESTRO_EXIT | PREMIUM_GATES_EXIT ))
if [ "$COMBINED_EXIT" -eq 0 ]; then
  echo "All Maestro tests passed."
else
  [ "$MAESTRO_EXIT" -ne 0 ]       && echo "Phase 1 (premium) tests failed (exit code $MAESTRO_EXIT)."
  [ "$PREMIUM_GATES_EXIT" -ne 0 ] && echo "Phase 2 (premium gates) tests failed (exit code $PREMIUM_GATES_EXIT)."
fi
exit $COMBINED_EXIT
