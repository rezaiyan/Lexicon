# Critical Risks & Invariants — Handle With Care

> Areas where small mistake causes outsized damage: data loss, broken learning progress, silent failures, stuck app state. Read relevant section before touching these systems.

---

## 1. Spaced Repetition Algorithm (`ReviewWordUseCase`)

**Must never break:**
- `repetitions` MUST reset to 0 when word advances level
- Ease factor bounds: min 1.3, max 2.5 — tuned for algorithm convergence
- Level 6 interval grows exponentially via `(word.interval * easeFactor).toInt()` capped at 365 days
- Quality value not 0 or 1 silently treated as FORGOT — intentional

**Traps:**
- `word.interval` must never be 0 at level 6. If it is, `0 * easeFactor = 0` → word reviews every minute forever
- Float-to-Int truncation on interval calc loses fractional days — don't add rounding without testing convergence
- `LEVEL_INTERVALS[level] ?: 1` silently falls back to 1 minute if level key missing from map — never remove entries

**Invariant:** `interval > 0` and `easeFactor > 0` must hold at all times on every Word in DB.

---

## 2. Analytics Session Lifecycle — MOST CRITICAL

**Fundamental risk:** Analytics sessions exist **only in memory**. App killed mid-session → all buffered review events permanently lost. No local persistence for pending sync.

**In `AnalyticsRecorderImpl`:**
- `startSession()` / `endSession()` use in-memory Mutex-protected map — no disk persistence
- On sync failure, session goes into `retryQueue` — also in memory, lost on process death
- `recordReviewEvent()` on non-existent session ID = **silent no-op** — no error, no warning

**In `ReviewViewModel`:**
- `beginAnalyticsSession()` launches `startSession()` as **fire-and-forget coroutine** — NOT awaited
- `reviewWord()` called immediately after; if `startSession` not complete, events recorded against session backend never saw
- Session settings (successesToAdvance, forgotPenalty) also loaded fire-and-forget — `reviewWord()` may run with defaults
- `onCleared()` launches `endAnalyticsSession()` without await — ViewModel destroyed quickly → final session may not send

**Do not:**
- Assume `startSession` completed before first `reviewWord` call
- Move `recordEvent()` outside `if (currentSessionId != null)` guard
- Remove Mutex from `AnalyticsRecorderImpl.sessions`
- Change `endSession` to throw on network failure (must succeed locally even when sync fails)

**Divergence risk:** `ReviewViewModel` computes `newLevel` locally (using `sessionSettings`) and sends to analytics. `ReviewWordUseCase` computes actual new level independently. If settings differ, analytics shows different level progressions than DB.

---

## 3. Auth Token Refresh — Race Conditions

**In `TokenRefreshManager`:**
- Uses Mutex for single-flight refresh (only one refresh in-flight at a time)
- After acquiring Mutex, double-checks if another caller already refreshed — do not remove this double-check
- Distinguishes `AuthenticationException` (clears tokens → forces logout) from transient errors (keeps tokens) — must preserve this distinction

**In `AuthInterceptor`:**
- Proactive refresh triggers when token expires in < 5 minutes — **fire-and-forget**, request continues with current token
- **Race condition:** Request may send with old token during proactive refresh, resulting in 401
- No 401-retry handler in interceptor — app relies on proactive refresh being early enough

**Token expiry trap:** Backend doesn't return `expiresIn` → `tokenExpiresAt` stored as 0 → `timeUntilExpiry` always negative → proactive refresh never triggers → user eventually hits 401 with no auto-recovery.

**Do not:**
- Make proactive refresh blocking (would deadlock concurrent requests)
- Remove `AuthenticationException` vs. transient-error distinction
- Change `getTokenExpiresAt()` to suspend function (called from synchronous context)

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
- `hasCompletedOnboarding()` is persisted flag — can be true even when user has no auth tokens (e.g., reinstall with Keychain surviving). Code handles this edge case explicitly; don't remove it.
- `getTotalCount()` failing returns 0 by default — sends user to Onboarding instead of skipping. Safe fallback.
- `markOnboardingCompleted()` called in multiple branches — if fails, subsequent logic assumes success. No compensation.

**Invariant:** `AppUiState.Ready` reached only if user authenticated AND onboarding complete. Never transition to Ready without both checks.

---

## 5. Word Sync & Deletion

**Sync strategy:** Backend = source of truth. `syncRemoteToLocal()` fetches remote words and resolves conflicts. Conflict resolver must remain deterministic — same inputs always pick same winner.

**Delete ordering:** Delete from backend FIRST, then local. Intentional — local delete fails after backend success → word re-appears on next sync (safe). Reverse (local first) → permanent phantom word.

**Partial failure risk:** `BatchUpdateLanguagesUseCase` updates words one by one. Fails on word #3 → words 1–2 updated and synced, 3–N not. No rollback. Code touching batch updates must tolerate partial success.

**Widget:** `DeleteWordsUseCase` refreshes daily widget after deletion via fire-and-forget. Widget refresh failure doesn't roll back deletion.

---

## 9. Tag System — Cascades & Reactivity

**Cascade ordering in `deleteTag()`:**
`TagLocalDataSource.deleteTag()` runs SQLDelight transaction: `deleteWordTagsForTag(id)` first, then `deleteTag(id)`. Reversing violates implicit FK constraint and leaves orphan `WordTagEntity` rows that can never be cleaned up.

**Atomic assignment in `setWordTags()`:**
`deleteWordTagsForWord(wordId)` followed by per-tag `insertWordTag()` calls — all inside one SQLDelight transaction. Adding error handling that returns early inside this transaction without rollback → words end up with no tags instead of previous assignment.

**Reactivity blast radius:**
Word list flows (`getAllWords`, `getDueCards`) use `combine()` with `countWordTags()` trigger. Every tag assignment/unassignment re-emits entire word list to all subscribers. Batch-assign operations must use `setWordTags()` (single transaction) not repeated `addTagToWord()` calls.

**Remote sync divergence risk:**
`syncTagsFromRemote()` calls `replaceAllTags()` (delete-all + re-insert). Wipes all local tag metadata but does NOT touch `WordTagEntity`. If remote sync succeeds but subsequent remote call to re-fetch word-tag mappings fails → local DB has tags with no associations — silent data loss.

**Do not:**
- Split `setWordTags()` transaction into separate suspend calls
- Add tag reads outside `getTagIdsForWord()` path (bypasses reactive trigger)
- Remove `countWordTags()` combine trigger from word flows — breaks live tag-count updates on word list screen

---

## 6. Settings Persistence (`SettingsLocalDataSourceImpl`)

**Type conversion traps:**
- DB stores Boolean as `Long` (0L or 1L). Equality check is `!= 0L` — any non-zero reads as true. Never write value other than 0 or 1 for boolean fields.
- DB stores `Int` fields as `Long`. `.toInt()` casts without bounds checks. TTS speaker IDs, minimumDueCards, ID fields must fit in 32-bit signed range.
- TTS speech rate: DB stores as `Double`, domain uses `Float`. Round-trip loses precision — don't compare persisted and in-memory values with `==`.

**Race condition:** TTS voice preference upsert is read-then-write with no transaction. Concurrent `setVoice()` calls for same language can lose data.

---

## 7. iOS Keychain (`IOSKeychainSecureStorage`)

**Memory safety:**
- All CF allocations scoped inside `memScoped {}` blocks — don't lift CF object creation outside these blocks
- `CFDataRef` created by `toCFData()` NOT manually released — known minor leak per save operation
- Never call `CFRelease()` on pointer that could be null — `requireNotNull` calls before release are load-bearing

**Token loss risk:**
- Migration from NSUserDefaults to Keychain runs on every token read until migration flag saved. Flag fails to persist → migration re-runs (idempotent but causes redundant writes)
- Keychain reads have no timeout — if Keychain locked (OS update, backup), app blocks
- `tokenExpiresAt` stored in NSUserDefaults (not Keychain) — not encrypted, not available during OS backup restrictions

**Invariant:** `ensureMigrationCompleted()` must be called before any Keychain read. Don't add Keychain reads that bypass this call.

---

## 8. `Try<T>` Error Handling (`core/common/Try.kt`)

**Non-obvious contracts:**
- `CancellationException` **always re-thrown**, never wrapped in `Failure` — coroutine cancellation works because of this. Never catch `CancellationException` inside `Try` transform.
- `Error` (JVM fatal) also re-thrown — don't catch `Error` and wrap in `Failure`
- `map()` and `flatMap()` catch all `Throwable` except above — bug in transform lambda becomes silent `Failure`, not crash
- `recover()` swallows original exception — stack trace of original failure not preserved in new `Failure`

**Do not:**
- Use `getOrThrow()` without handling `CancellationException` at call site
- Chain `.map { }.map { }.map { }` deeply — any lambda throws, original context lost
- Catch `CancellationException` inside lambda passed to `Try` operators

---

## 10. Batch Remote Operations — Sequential Request Anti-Pattern

**Bug this section prevents:** Batch tag assignment originally looped `assignWordTags(wordId)` for each selected word, sending N sequential HTTP requests. Two failure modes caused partial updates:

1. **Fail-fast partial failure:** `forEach { remoteDataSource.updateWordTags(wordId, tagIds).getOrThrow() }` — word #3 fails → words 1–2 updated and synced, 3–N not. No rollback. Client and backend inconsistent.
2. **JPA `@Version` optimistic locking failure:** Original `updateWordTags` endpoint loaded `Word` entity, called `wordRepository.save(word)` to persist tag changes. `save()` bumps `@Version`. Under concurrent load (sync + batch-assign), version mismatch → `OptimisticLockingFailureException` → random words silently fail.

**Rules:**

- **Never loop N sequential HTTP requests for batch mutations.** Always add dedicated batch endpoint (e.g., `POST /words/batch-assign-tags`) that processes all items in single `@Transactional` on server.
- **Never modify join table data by loading + saving parent entity.** Use native `@Modifying @Query` directly on join table — bypasses parent entity's `@Version` entirely.
- **Client batch local writes must also be single transaction.** Use SQLDelight `queries.transaction { wordIds.forEach { ... } }` — not `wordIds.forEach { queries.transaction { ... } }` (one transaction per word).

**Correct backend pattern (Spring Boot):**
```kotlin
// In WordRepository.kt
@Modifying
@Query(
    "DELETE FROM word_tags WHERE word_id IN :wordIds " +
        "AND word_id IN (SELECT id FROM words WHERE user_id = :userId)",
    nativeQuery = true
)
fun deleteWordTagsByWordIdsAndUserId(wordIds: List<Long>, userId: Long)

@Modifying
@Query(
    "INSERT INTO word_tags (word_id, tag_id) " +
        "SELECT w.id, :tagId FROM words w WHERE w.id IN :wordIds AND w.user_id = :userId " +
        "ON CONFLICT DO NOTHING",
    nativeQuery = true
)
fun insertWordTagsByWordIdsAndUserId(wordIds: List<Long>, tagId: Long, userId: Long)

// In WordService.kt
@Transactional
fun batchAssignTags(user: User, wordIds: List<Long>, tagIds: List<Long>): Int {
    wordRepository.deleteWordTagsByWordIdsAndUserId(wordIds, user.id!!)
    tagIds.forEach { tagId ->
        wordRepository.insertWordTagsByWordIdsAndUserId(wordIds, tagId, user.id!!)
    }
    return wordIds.size
}
```

**Correct client pattern (KMP):**
```kotlin
// UseCase — single batch call, not a loop
override suspend fun invoke(params: BatchAssignTagsParams): Try<Int> = Try {
    tagRepository.batchAssignWordTags(
        wordIds = params.wordIds.map { it.toLong() },
        tagIds = params.tagIds
    ).getOrThrow()
    params.wordIds.size
}

// Local data source — single SQLDelight transaction
override suspend fun batchSetWordTags(wordIds: List<Long>, tagIds: List<Long>) {
    queries.transaction {
        wordIds.forEach { wordId ->
            queries.deleteWordTagsForWord(wordId)
            tagIds.forEach { tagId -> queries.insertWordTag(wordId, tagId) }
        }
    }
}
```

**Do not:**
- Use `forEach { singleItemRepository.doX(id).getOrThrow() }` for any remote batch operation
- Call `wordRepository.save(entity)` just to persist join table changes — use native SQL `@Modifying @Query` on join table directly
- Wrap each item in its own `queries.transaction {}` inside loop

---

## Risk Summary

| Area | Risk | Impact |
|------|------|--------|
| Analytics session (in-memory only) | **CRITICAL** | Silent loss of all review data on crash |
| ReviewViewModel startSession fire-and-forget | **CRITICAL** | Events orphaned to unregistered sessions |
| ReviewWordUseCase interval=0 at level 6 | **HIGH** | Mastered word reviews every minute forever |
| Token refresh race condition | **HIGH** | User hits 401 despite valid session |
| Tag cascade delete ordering | **HIGH** | Orphan WordTagEntity rows that can never be cleaned up |
| Tag syncTagsFromRemote divergence | **HIGH** | Tag assignments silently lost if word-tag re-fetch fails |
| AppNavigationViewModel onboarding flag | **MEDIUM** | User stuck in wrong screen after reinstall |
| Batch remote op N sequential requests | **HIGH** | Partial failure leaves client/server inconsistent, no rollback |
| JPA @Version bump on join-table-only save() | **HIGH** | OptimisticLockingFailureException under concurrent load, random items silently fail |
| Batch word update partial failure | **MEDIUM** | Inconsistent local/remote state |
| Tag setWordTags partial transaction | **MEDIUM** | Words left with no tags instead of previous assignment |
| Tag reactivity blast radius (N re-emissions) | **MEDIUM** | UI lag when bulk-assigning tags via addTagToWord loop |
| Settings Long→Int cast | **MEDIUM** | Corrupted speaker ID on large ID values |
| iOS Keychain CFDataRef leak | **LOW** | Memory accumulation over many sessions |
| Try<T> transform error masking | **LOW** | Hard-to-debug silent failures |