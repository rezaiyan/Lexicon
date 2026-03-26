# BUG-4 — `GET /profile-stats` Fires Twice on Profile Screen Open

**Priority:** P1
**Status:** Open

## Observed Behaviour

Two `GET /users/profile-stats` requests fire 44ms apart every time the Profile screen opens:

```
→ GET  https://api.vokab.app/users/profile-stats   (09:45:32.799)
→ GET  https://api.vokab.app/users/profile-stats   (09:45:32.843)
```

## Root Cause

Two independent triggers both call `loadProfileStats()` on the first screen visit:

**Trigger 1 — ViewModel init:**
```kotlin
// ProfileViewModel.kt:61
init {
    observeUser()
    observeStreak()
    observeFeatureAccess()
    viewModelScope.launch { loadProfileStats() }   // ← fires immediately
}
```

**Trigger 2 — Screen LaunchedEffect:**
```kotlin
// ProfileScreen.kt:47
LaunchedEffect(Unit) {
    profileViewModel.refreshProfileStats()   // ← also fires on first composition
}
```

`ThrottledAction` (60s interval) does **not** suppress the first invocation — both calls go through on initial open.

**Files:**
- `feature/profile/src/commonMain/kotlin/feature/profile/ProfileViewModel.kt:61`
- `feature/profile/src/commonMain/kotlin/feature/profile/ui/ProfileScreen.kt:47`

## Fix

Remove `viewModelScope.launch { loadProfileStats() }` from `ProfileViewModel.init`.

The `LaunchedEffect(Unit)` in `ProfileScreen` already handles the initial load on every screen visit. The ViewModel `init` does not need to pre-fetch — it should only set up observers.

```kotlin
// ProfileViewModel.kt — BEFORE
init {
    observeUser()
    observeStreak()
    observeFeatureAccess()
    viewModelScope.launch { loadProfileStats() }   // ← remove this line
}

// ProfileViewModel.kt — AFTER
init {
    observeUser()
    observeStreak()
    observeFeatureAccess()
    // Initial load driven by LaunchedEffect in ProfileScreen
}
```

## Acceptance Criteria

- Exactly **1** `GET /profile-stats` fires when navigating to the Profile screen
- Pull-to-refresh still triggers a new fetch (via `refreshProfileStats()`)
- The 60s throttle on `refreshProfileStats()` still applies for rapid re-visits
