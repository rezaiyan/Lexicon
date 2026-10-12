#!/usr/bin/env bash
# Runs the Maestro suite on the CI emulator: install, inject CI auth tokens, test, uninstall.
#
# Called from the android-emulator-runner `script:`. That action runs each script line in its
# own shell, so variables (like a saved exit code) don't survive between lines; keeping the
# whole run in this one script is what lets a failing suite fail the job.
#
# Each flow runs as its own `maestro test`, in the order of `flowsOrder` in maestro/config.yaml.
# The CI emulator is flaky in ways that have nothing to do with the app:
#   - Maestro's on-device server dies mid-run; in a single run that fails every remaining flow,
#     a fresh process per flow reconnects.
#   - The emulator itself can vanish ("device 'emulator-5554' not found"); it is relaunched once,
#     with the app reinstalled and re-authenticated, before the next flow.
#   - The first text input after the keyboard appears is sometimes dropped.
# So a failed flow is retried once; it only counts as failed if the retry fails too.
#
# Required env: CI_ACCESS_TOKEN, CI_REFRESH_TOKEN, CI_EXPIRES_IN_MS
# Writes: $HOME/maestro-reports/*.xml (JUnit, one per flow), $HOME/maestro-debug/<flow>/

set -uo pipefail

: "${CI_ACCESS_TOKEN:?CI_ACCESS_TOKEN is required}"
: "${CI_REFRESH_TOKEN:?CI_REFRESH_TOKEN is required}"
: "${CI_EXPIRES_IN_MS:?CI_EXPIRES_IN_MS is required}"

PACKAGE="com.alirezaiyan.vokab"
AVD_NAME="test" # android-emulator-runner's AVD
REPORTS="$HOME/maestro-reports"
DEBUG="$HOME/maestro-debug"
EMULATOR_RESTARTS_LEFT=1

setup_app() {
  adb install -r composeApp/build/outputs/apk/debug/*.apk || return 1

  # Launch once so Koin is initialised before the token broadcast arrives.
  adb shell am start -n "$PACKAGE/.MainActivity"
  sleep 8
  adb shell "am broadcast -a $PACKAGE.CI_INJECT_TOKENS -n $PACKAGE/$PACKAGE.CiTokenReceiver \
    --es accessToken '$CI_ACCESS_TOKEN' --es refreshToken '$CI_REFRESH_TOKEN' --el expiresInMs $CI_EXPIRES_IN_MS"
  sleep 2
  adb shell am force-stop "$PACKAGE"
  sleep 2
}

device_online() {
  [ "$(adb get-state 2>/dev/null)" = "device" ] &&
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]
}

restart_emulator() {
  echo "::warning::Emulator is gone; relaunching it"
  pkill -f qemu-system || true
  sleep 5
  adb kill-server || true
  "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" -no-window -gpu swiftshader_indirect \
    -no-snapshot -noaudio -no-boot-anim > "$HOME/emulator-restart.log" 2>&1 &
  adb start-server
  timeout 300 adb wait-for-device || return 1
  for _ in $(seq 1 150); do
    device_online && break
    sleep 2
  done
  device_online || return 1
  adb shell input keyevent 82
  for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb shell settings put global "$setting" 0.0
  done
  setup_app
}

# Returns non-zero only when the device is gone and can't be brought back.
ensure_device() {
  device_online && return 0
  [ "$EMULATOR_RESTARTS_LEFT" -gt 0 ] || return 1
  EMULATOR_RESTARTS_LEFT=$((EMULATOR_RESTARTS_LEFT - 1))
  restart_emulator
}

# Prints what was on screen and the device's warnings/errors for a flow that failed for good, so
# the cause is readable from the job log without downloading the artifacts.
dump_failure() { # <flow>
  echo "::group::Diagnostics: $1"
  echo "--- on screen (text / content descriptions) ---"
  maestro hierarchy 2>/dev/null |
    jq -r '.. | objects | .attributes? // empty | .text, .accessibilityText, ."hintText" | select(. != null and . != "")' |
    awk '!seen[$0]++' | head -120
  echo "--- logcat (warnings and errors) ---"
  adb logcat -d -v brief '*:W' 2>/dev/null | grep -vE 'chatty|GnssHAL|ColorBuffer|goldfish|EGL_emulation' | tail -120
  echo "::endgroup::"
}

run_flow() { # <flow> <name>
  rm -rf "${DEBUG:?}/$2"
  adb logcat -c 2>/dev/null || true
  maestro test --format junit --output "$REPORTS/$2.xml" \
    --debug-output "$DEBUG/$2" --flatten-debug-output "maestro/$1"
}

setup_app || exit 1

mapfile -t FLOWS < <(sed -n '/^ *flowsOrder:/,$ s|^ *- \(flows/.*\.yaml\) *$|\1|p' maestro/config.yaml)
[ "${#FLOWS[@]}" -gt 0 ] || { echo "::error::No flows found in maestro/config.yaml flowsOrder"; exit 1; }

mkdir -p "$REPORTS" "$DEBUG"
failed=()
flaky=()

for flow in "${FLOWS[@]}"; do
  name="$(echo "$flow" | sed 's|^flows/||; s|\.yaml$||; s|/|__|g')"
  echo "::group::$flow"
  if ! ensure_device; then
    echo "::error::Emulator lost and could not be restarted; skipping $flow"
    failed+=("$flow")
  elif ! run_flow "$flow" "$name"; then
    echo "::warning::$flow failed; retrying once"
    if ensure_device && run_flow "$flow" "$name"; then
      flaky+=("$flow")
    else
      failed+=("$flow")
      echo "::endgroup::"
      dump_failure "$flow"
      continue
    fi
  fi
  echo "::endgroup::"
done

adb uninstall "$PACKAGE" || true

echo "Maestro: $(( ${#FLOWS[@]} - ${#failed[@]} ))/${#FLOWS[@]} flows passed"
if [ "${#flaky[@]}" -gt 0 ]; then
  printf '  passed on retry: %s\n' "${flaky[@]}"
fi
if [ "${#failed[@]}" -gt 0 ]; then
  printf '  failed: %s\n' "${failed[@]}"
  exit 1
fi
