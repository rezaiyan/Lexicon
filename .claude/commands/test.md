---
description: Run project tests (common, Android, or all)
argument-hint: "[common|android|all]"
allowed-tools: ["Bash", "Read", "Glob", "Grep"]
---

Run tests for the Lexicon project based on the argument provided.

## Test Commands

- If argument is "common" or empty: `./gradlew composeApp:cleanAllTests composeApp:allTests`
- If argument is "android": `./gradlew composeApp:testDebugUnitTest`
- If argument is "all": Run both common and Android tests sequentially

## Instructions

1. Run the appropriate Gradle test command(s)
2. Wait for completion and capture output
3. If tests fail, analyze the failures and provide a summary:
   - Which tests failed
   - The assertion errors or exceptions
   - Suggest likely fixes based on the test names and error messages
4. If all tests pass, confirm with a concise summary of test counts
