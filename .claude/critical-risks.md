# Critical Risks & Invariants — Handle With Care

> These are the areas where a small mistake causes outsized damage: data loss, broken learning progress, silent failures, or stuck app state. Read the relevant section before touching any of these systems.

---

## 1. Spaced Repetition Algorithm (`ReviewWordUseCase`)

**What must never break:**
- `repetitions` MUST reset to 0 whenever the word advances a level
- Ease factor bounds: min 1.3, max 2.5 — these are tuned for algorithm convergence
- Level 6 interval grows exponentially via `(word.interval * easeFactor).toInt()` capped at 365 days
- Any quality value that is not 0 or 1 is silently treated as FORGOT — this is intentional

**Traps:**
- `word.interval` must never be 0 at level 6. If it is, `0 * easeFactor = 0` → word reviews every minute forever
- Float-to-Int truncation on the interval calculation loses fractional days — do not add rounding without testing convergence
- `LEVEL_INTERVALS[level] ?: 1` silently falls back to 1 minute if the level key is missing from the map — never remove entries from that map

**Invariant:** `interval > 0` and `easeFactor > 0` must hold at all times on every Word in the DB.

---

## 2. Analytics Session Lifecycle — MOST CRITICAL

**The fundamental risk:** Analytics sessions exist **only in memory**. If the app is killed mid-session, all buffered review events are permanently lost. There is no local persistence for pending sync.

**In `AnalyticsRecorderImpl`:**
- `startSession()` / `endSession()` use an in-memory Mutex-protected map — no disk persistence
- On sync failure, the session goes into `retryQueue` — but retry queue is also in memory and is lost on process death
- `recordReviewEvent()` on a non-existent session ID is a **silent no-op** — no error, no warning

**In `ReviewViewModel`:**
- `beginAnalyticsSession()` launches `startSession()` as a **fire-and-forget coroutine** — it is NOT awaited
- `reviewWord()` is called immediately after; if `startSession` hasn't completed yet, events are recorded against a session the backend has never seen
- Session settings (successesToAdvance, forgotPenalty) are also loaded fire-and-forget — `reviewWord()` may run with defaults if settings haven't loaded yet
- `onCleared()` launches `endAnalyticsSession()` without awaiting it — if the ViewModel is destroyed quickly, the final session may not be sent

**Do not:**
- Assume `startSession` has completed before `reviewWord` is first called
- Move `recordEvent()` outside the `if (currentSessionId != null)` guard
- Remove the Mutex from `AnalyticsRecorderImpl.sessions`
- Change `endSession` to throw on network failure (it must succeed locally even when sync fails)

**The divergence risk:** `ReviewViewModel` computes `newLevel` locally (using `sessionSettings`) and sends it to analytics. `ReviewWordUseCase` computes the actual new level independently. If settings differ between the two, analytics shows different level progressions than what's in the DB.

---

## 3. Auth Token Refresh — Race Conditions

**In `TokenRefreshManager`:**
- Uses a Mutex for single-flight refresh (only one refresh in-flight at a time)
- After acquiring Mutex, double-checks if another caller already refreshed — do not remove this double-check
- Distinguishes `AuthenticationException` (clears tokens → forces logout) from transient errors (keeps tokens) — this distinction must be preserved

**In `AuthInterceptor`:**
- Proactive refresh triggers when token expires in < 5 minutes — this is **fire-and-forget**, the request continues with the current token
- **Race condition:** A request may be sent with an old token even during a proactive refresh, resulting in a 401
- There is no 401-retry handler in the interceptor — the app relies on proactive refresh being early enough

**Token expiry trap:** If the backend does not return `expiresIn`, `tokenExpiresAt` is stored as 0. Then `timeUntilExpiry` is always negative, proactive refresh never triggers, and the user eventually hits a 401 with no auto-recovery.

**Do not:**
- Make the proactive refresh blocking (would deadlock concurrent requests)
- Remove the `AuthenticationException` vs. transient-error distinction
- Change `getTokenExpiresAt()` to a suspend function (it is called from a synchronous context)

---

## 4. App Navigation State Machine (`AppNavigationViewModel`)

**State transitions:**
```
Auth(Verifying) → Auth(LoginRequired) | Auth(NeedsOnboardingCheck) | Ready
Auth(NeedsOnboardingCheck) → Onboarding | Ready   (based on word count)
Onboarding → VocabularyPreview → Ready
Any → Auth(LoginRequired)  [on logout]
```

**Traps:**
- `hasCompletedOnboarding()` is a persisted flag — it can be true even when the user has no auth tokens (e.g., reinstall with Keychain surviving). The code handles this edge case explicitly; do not remove it.
- `getTotalCount()` failing returns 0 by default — this sends the user to Onboarding instead of skipping it. This is the safe fallback.
- `markOnboardingCompleted()` is called in multiple branches — if it fails, subsequent logic assumes it succeeded. There is no compensation.

**Invariant:** `AppUiState.Ready` must only be reached if the user is authenticated AND onboarding is complete. Never transition to Ready without both checks passing.

---

## 5. Word Sync & Deletion

**Sync strategy:** Backend is source of truth. `syncRemoteToLocal()` fetches remote words and resolves conflicts. The conflict resolver must remain deterministic — same inputs must always pick the same winner.

**Delete ordering:** Delete from backend FIRST, then local. This is intentional — if local delete fails after backend success, the word re-appears on next sync (safe). The reverse (local first) would cause a permanent phantom word.

**Partial failure risk:** Batch language updates (`BatchUpdateLanguagesUseCase`) update words one by one. If it fails on word #3, words 1–2 are updated and synced, words 3–N are not. There is no rollback. Code touching batch updates must tolerate partial success.

**Widget:** `DeleteWordsUseCase` refreshes the daily widget after deletion via a fire-and-forget call. Widget refresh failure does not roll back the deletion.

---

## 6. Settings Persistence (`SettingsLocalDataSourceImpl`)

**Type conversion traps:**
- DB stores Boolean as `Long` (0L or 1L). The equality check is `!= 0L` — any non-zero value reads as true. Never write a value other than 0 or 1 for boolean fields.
- DB stores `Int` fields as `Long`. There are `.toInt()` casts without bounds checks. TTS speaker IDs, minimumDueCards, and ID fields must fit in 32-bit signed range.
- TTS speech rate: DB stores as `Double`, domain uses `Float`. The round-trip loses precision — do not compare persisted and in-memory values with `==`.

**Race condition:** TTS voice preference upsert is a read-then-write pattern with no transaction. Concurrent calls to `setVoice()` for the same language can lose data.

---

## 7. iOS Keychain (`IOSKeychainSecureStorage`)

**Memory safety:**
- All CF allocations are scoped inside `memScoped {}` blocks — do not lift CF object creation outside these blocks
- `CFDataRef` created by `toCFData()` is NOT manually released — this is a known minor leak per save operation
- Never call `CFRelease()` on a pointer that could be null — the `requireNotNull` calls before release are load-bearing

**Token loss risk:**
- Migration from NSUserDefaults to Keychain runs on every token read until the migration flag is saved. If the flag fails to persist, migration re-runs (idempotent but causes redundant writes)
- Keychain reads have no timeout — if the Keychain is locked (OS update, backup), the app will block
- `tokenExpiresAt` is stored in NSUserDefaults (not Keychain) — it is not encrypted and not available during OS backup restrictions

**Invariant:** `ensureMigrationCompleted()` must be called before any Keychain read. Do not add Keychain reads that bypass this call.

---

## 8. `Try<T>` Error Handling (`core/common/Try.kt`)

**Non-obvious contracts:**
- `CancellationException` is **always re-thrown**, never wrapped in `Failure` — coroutine cancellation works correctly because of this. Never catch `CancellationException` inside a `Try` transform.
- `Error` (JVM fatal) is also re-thrown — do not catch `Error` and wrap it in `Failure`
- `map()` and `flatMap()` catch all `Throwable` except the above — a bug in a transform lambda becomes a silent `Failure`, not a crash
- `recover()` swallows the original exception — the stack trace of the original failure is not preserved in the new `Failure`

**Do not:**
- Use `getOrThrow()` without handling `CancellationException` at the call site
- Chain `.map { }.map { }.map { }` deeply — if any lambda throws, the original context is lost
- Catch `CancellationException` inside a lambda passed to `Try` operators

---

## Risk Summary

| Area | Risk | Impact |
|------|------|--------|
| Analytics session (in-memory only) | **CRITICAL** | Silent loss of all review data on crash |
| ReviewViewModel startSession fire-and-forget | **CRITICAL** | Events orphaned to unregistered sessions |
| ReviewWordUseCase interval=0 at level 6 | **HIGH** | Mastered word reviews every minute forever |
| Token refresh race condition | **HIGH** | User hits 401 despite valid session |
| AppNavigationViewModel onboarding flag | **MEDIUM** | User stuck in wrong screen after reinstall |
| Batch word update partial failure | **MEDIUM** | Inconsistent local/remote state |
| Settings Long→Int cast | **MEDIUM** | Corrupted speaker ID on large ID values |
| iOS Keychain CFDataRef leak | **LOW** | Memory accumulation over many sessions |
| Try<T> transform error masking | **LOW** | Hard-to-debug silent failures |
