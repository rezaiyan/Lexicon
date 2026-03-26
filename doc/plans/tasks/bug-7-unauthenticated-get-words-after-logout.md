# BUG-7 — `GET /words` Fires Unauthenticated (401) After Logout

**Priority:** P1
**Status:** Open

## Observed Behaviour

165ms after logout completes, a `GET /words` fires with no `Authorization` header and receives a 401:

```
→ DELETE  https://api.vokab.app/notifications/tokens   ← logout step 1
← 200     /notifications/tokens (87ms)

→ POST    https://api.vokab.app/auth/logout             ← logout step 2
← 200     /auth/logout (54ms)

[165ms gap]

→ GET     https://api.vokab.app/words                  ← unexpected, no auth header
← 401     /words (23ms)
```

## Root Cause

Logout clears the auth token, which causes an auth state change. This state change is observed by something that triggers `syncWithRemote()` without first checking whether a valid auth token exists.

Most likely culprit: `StudyProgressViewModel` re-initializes when navigation returns to the home/study screen post-logout. Its `init` calls `getProgressStats()` → `launch { syncWithRemote() }` → `GET /words`. At this point the token has already been cleared, so the request goes out unauthenticated.

**Files to investigate:**
- `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt` — `init` calls `getProgressStats()` → `launch { syncWithRemote() }`
- `data/src/commonMain/kotlin/data/word/repository/WordRepositoryImpl.kt:207-210` — `syncWithRemote()` calls `GET /words`

## Fix

Guard `syncWithRemote()` against unauthenticated calls. Before issuing the network request, check that a valid auth token is present:

```kotlin
// WordRepositoryImpl
suspend fun syncWithRemote() {
    if (!authTokenProvider.hasValidToken()) return   // skip if not authenticated
    // ... existing sync logic
}
```

Alternatively, guard at the call site in `StudyProgressViewModel`:

```kotlin
// StudyProgressViewModel
private fun getProgressStats() {
    viewModelScope.launch {
        if (!userManager.isAuthenticated()) return@launch
        syncWithRemote()
        // ...
    }
}
```

Also verify that `StudyProgressViewModel` is not created/re-created during the logout navigation transition. If it is, consider clearing/cancelling its coroutines as part of the logout flow before navigation occurs.

## Acceptance Criteria

- **0** network calls fire after `POST /auth/logout` completes (until the next login)
- No 401 errors appear in logs during/after logout
- The local word DB is not affected by the logout guard (local reads still work)
