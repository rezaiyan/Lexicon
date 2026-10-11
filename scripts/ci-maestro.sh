#!/usr/bin/env bash
# Runs the Maestro suite on the CI emulator: install, inject CI auth tokens, test, uninstall.
#
# Called from the android-emulator-runner `script:`. That action runs each script line in its
# own shell, so variables (like a saved exit code) don't survive between lines; keeping the
# whole run in this one script is what lets a failing suite fail the job.
#
# Required env: CI_ACCESS_TOKEN, CI_REFRESH_TOKEN, CI_EXPIRES_IN_MS
# Writes: $HOME/maestro-report.xml, $HOME/maestro-debug/

set -uo pipefail

: "${CI_ACCESS_TOKEN:?CI_ACCESS_TOKEN is required}"
: "${CI_REFRESH_TOKEN:?CI_REFRESH_TOKEN is required}"
: "${CI_EXPIRES_IN_MS:?CI_EXPIRES_IN_MS is required}"

PACKAGE="com.alirezaiyan.vokab"

adb install composeApp/build/outputs/apk/debug/*.apk || exit 1

# Launch once so Koin is initialised before the token broadcast arrives.
adb shell am start -n "$PACKAGE/.MainActivity"
sleep 8
adb shell "am broadcast -a $PACKAGE.CI_INJECT_TOKENS -n $PACKAGE/$PACKAGE.CiTokenReceiver \
  --es accessToken '$CI_ACCESS_TOKEN' --es refreshToken '$CI_REFRESH_TOKEN' --el expiresInMs $CI_EXPIRES_IN_MS"
sleep 2
adb shell am force-stop "$PACKAGE"
sleep 2

maestro test --format junit --output "$HOME/maestro-report.xml" \
  --debug-output "$HOME/maestro-debug" --flatten-debug-output maestro/
status=$?

adb uninstall "$PACKAGE" || true
exit $status
