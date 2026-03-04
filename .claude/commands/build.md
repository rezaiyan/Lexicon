---
description: Build the project (Android APK or iOS framework)
argument-hint: "[android|ios|both]"
allowed-tools: ["Bash", "Read"]
---

Build Lexicon based on the argument provided.

## Build Commands

- If argument is "android" or empty: `./gradlew composeApp:assembleDebug`
- If argument is "ios": `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64`
- If argument is "both": Run Android build first, then iOS

## Instructions

1. Run the appropriate Gradle build command(s)
2. Report build success or failure
3. On failure, analyze the build errors and suggest fixes
4. On success, report the output artifact location
