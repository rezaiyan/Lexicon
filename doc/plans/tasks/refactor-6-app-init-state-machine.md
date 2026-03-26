# REFACTOR-6 — Extract App Init State Machine from AppNavigationViewModel

**Priority:** P1
**Status:** Deferred — requires dedicated planning session

## Why Deferred

`critical-risks.md §4` explicitly documents this state machine as a high-risk area with
non-obvious invariants that must be preserved. Extracting it without full test coverage first
risks breaking the app startup flow for edge-case users (e.g., reinstall with Keychain surviving).

This task must NOT be started until:
1. A dedicated planning session reviews the invariants below
2. Tests covering all branches of the current ViewModel behaviour are written first (TDD)
3. The extraction is done with tests green at every step

---

## Problem

`AppNavigationViewModel` makes onboarding/auth routing decisions inline:

```kotlin
// AppNavigationViewModel.kt ~lines 26–48
val onboardingCompleted = onboardingRepository.hasCompletedOnboarding().getOrDefault(false)
when {
    onboardingCompleted && isAuthenticated -> updateState { AppUiState.Ready }
    onboardingCompleted -> updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired) }
    isAuthenticated -> {
        // User has valid session but lost onboarding flag → returning user, skip onboarding
        onboardingRepository.markOnboardingCompleted()
        updateState { AppUiState.Ready }
    }
    else -> updateState { AppUiState.Auth(...) }
}
```

```kotlin
// ~lines 70–79
val wordCount = wordRepository.getTotalCount().getOrElse { 0 }
if (wordCount > 0) {
    onboardingRepository.markOnboardingCompleted()
    updateState { AppUiState.Ready }
} else {
    updateState { AppUiState.Onboarding }
}
```

These are **business rules**, not UI decisions:
- "Authenticated + onboarding complete → Ready"
- "Authenticated + onboarding flag missing + has existing data → treat as returning user, mark complete, go Ready"
- "Authenticated + no data → go Onboarding"
- "Not authenticated → LoginRequired"

---

## Invariants That Must Be Preserved

From `critical-risks.md §4`:

1. **Reinstall with Keychain surviving** — `hasCompletedOnboarding()` can return `true` even
   with no auth tokens (Keychain survives reinstall, onboarding flag does not reset).
   The current code handles this: if authenticated=false AND onboarding=true → LoginRequired.
   The extracted use case must preserve this path exactly.

2. **`getTotalCount()` failure = 0** — if the word count query fails, `getOrElse { 0 }` sends
   the user to Onboarding (safe: they'll set up preferences again). This is intentional.
   The extracted use case must preserve this fallback.

3. **`markOnboardingCompleted()` has no compensation** — if it fails, subsequent logic assumes
   it succeeded. The extracted use case must not add retry logic or throw on failure.

4. **`AppUiState.Ready` invariant** — must only be reached if BOTH authenticated AND onboarding
   complete. The use case result must encode this as a type-safe guarantee.

---

## Proposed Design (subject to planning session)

```
domain/src/commonMain/kotlin/domain/auth/usecase/DetermineAppInitStateUseCase.kt
```
```kotlin
class DetermineAppInitStateUseCase(
    private val onboardingRepository: IOnboardingRepository,
    private val wordRepository: IWordRepository,
)

sealed interface AppInitState {
    data object Ready : AppInitState
    data object NeedsOnboarding : AppInitState
    data object LoginRequired : AppInitState
}

// invoke(isAuthenticated: Boolean): AppInitState
```

```
domain/src/commonMain/kotlin/domain/auth/usecase/HandleAuthCompleteUseCase.kt
```
Encapsulates the `onAuthCompleteCheckingData()` branch (word count check → mark complete or onboard).

---

## Pre-Implementation Checklist

Before writing any production code:

- [ ] Read `critical-risks.md §4` fully
- [ ] Write `AppNavigationViewModelTest` covering ALL current branches:
  - authenticated + onboarding complete → Ready
  - authenticated + onboarding missing + has data → mark complete, Ready
  - authenticated + onboarding missing + no data → Onboarding
  - not authenticated + onboarding complete → LoginRequired
  - not authenticated + onboarding missing → LoginRequired
  - `getTotalCount()` failure → Onboarding (safe fallback)
- [ ] Confirm tests pass against the CURRENT (unmodified) ViewModel
- [ ] Only then extract to use case and re-run tests

## Files to Create (after pre-implementation)

```
domain/src/commonMain/kotlin/domain/auth/usecase/DetermineAppInitStateUseCase.kt
domain/src/commonMain/kotlin/domain/auth/usecase/HandleAuthCompleteUseCase.kt
domain/src/commonMain/kotlin/domain/auth/model/AppInitState.kt
```

## Files to Modify (after pre-implementation)

```
presentation/.../AppNavigationViewModel.kt
    — onSessionVerified(): delegate to DetermineAppInitStateUseCase, map result to AppUiState
    — onAuthCompleteCheckingData(): delegate to HandleAuthCompleteUseCase

composeApp/.../di/AppModule.kt
    — factoryOf(::DetermineAppInitStateUseCase)
    — factoryOf(::HandleAuthCompleteUseCase)
```

## Tests to Create

```
domain/src/commonTest/kotlin/domain/auth/DetermineAppInitStateUseCaseTest.kt
domain/src/commonTest/kotlin/domain/auth/HandleAuthCompleteUseCaseTest.kt
presentation/src/commonTest/kotlin/.../AppNavigationViewModelTest.kt
```

## Acceptance Criteria

- [ ] All 6 branch scenarios tested before extraction begins
- [ ] All tests remain green after extraction
- [ ] `AppUiState.Ready` unreachable without authenticated=true AND onboarding=true in use case
- [ ] `getTotalCount()` failure maps to `NeedsOnboarding` in `HandleAuthCompleteUseCase`
- [ ] `markOnboardingCompleted()` failure is silent (no compensation, no throw)
- [ ] `critical-risks.md §4` re-read and signed off before merge
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
