# BUG-2 — `GET /words` Fires Twice on Login (300ms Apart)

**Priority:** P1
**Status:** Open

## Observed Behaviour

Two `GET /words` requests fire 300ms apart immediately after Google login:

```
→ GET  https://api.vokab.app/words   (09:33:17.691)  ← auth path
→ GET  https://api.vokab.app/words   (09:33:17.997)  ← screen path
```

The second request fetches data that was already fresh from 300ms prior.

## Root Cause

Two independent code paths both call `syncWithRemote()` → `WordRemoteDataSource.getWords()`:

**Path 1 — Auth path (correct):**
```
AuthViewModel.onLoginSuccess()
  → HandleLoginSuccessUseCase
    → SyncRemoteToLocalUseCase
      → syncWithRemote()
        → GET /words
```
File: `domain/src/commonMain/kotlin/domain/auth/usecase/HandleLoginSuccessUseCase.kt:28`

**Path 2 — Screen path (redundant):**
```
StudyProgressViewModel.init
  → getProgressStats()
    → launch { syncWithRemote() }
      → GET /words
```
Files:
- `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt:55-57`
- `data/src/commonMain/kotlin/data/word/repository/WordRepositoryImpl.kt:207-210`

The post-login sync already delivers fresh words. The screen-initiated sync 300ms later is redundant.

## Fix

Add a `lastSyncedAt: Long` timestamp to `WordRepositoryImpl` (or a shared sync state object). After `syncWithRemote()` completes, record `System.currentTimeMillis()`.

In `getProgressStats()` (and anywhere else that calls `syncWithRemote()` proactively), skip the call if `now - lastSyncedAt < FRESH_THRESHOLD_MS` (e.g. 30 seconds).

```kotlin
// WordRepositoryImpl
private var lastSyncedAt: Long = 0L
private val FRESH_THRESHOLD_MS = 30_000L

suspend fun syncWithRemote() {
    if (System.currentTimeMillis() - lastSyncedAt < FRESH_THRESHOLD_MS) return
    // ... existing sync logic
    lastSyncedAt = System.currentTimeMillis()
}
```

## Acceptance Criteria

- Exactly **1** `GET /words` fires during the login → home screen flow
- Manual pull-to-refresh still triggers a sync regardless of the timestamp
- The timestamp is reset on logout so the next login triggers a fresh sync
