# BUG-5 — `PATCH /words/{id}` Fires Once Per Card Answer Instead of Batching

**Priority:** P1
**Status:** Open

## Observed Behaviour

During a review session, each card answer triggers an immediate network request:

```
→ PATCH  https://api.vokab.app/words/abc123   (09:50:03.679)   ← card 1 answered
→ PATCH  https://api.vokab.app/words/def456   (09:50:07.162)   ← card 2 answered
→ PATCH  https://api.vokab.app/words/ghi789   (09:50:08.999)   ← card 3 answered
→ POST   https://api.vokab.app/analytics/sync (09:50:13.586)   ← session close
```

3 separate PATCH calls for a 3-card session. For a 20-card session this becomes 20 requests.

## Root Cause

Each card answer immediately calls the repository to sync SRS state (due date, ease factor, interval) to the server. The call is not deferred or batched — it fires as soon as the user taps an answer.

Card SRS data does not need to reach the server mid-session. It is only needed when the session ends so the server can schedule future reviews.

## Fix

Buffer card answer results in memory during the review session. Flush to the server when the session closes — alongside `POST /analytics/sync`.

**Step 1 — Buffer locally:**
In the review session ViewModel (or use case), accumulate answered cards in a list rather than calling the repository immediately.

```kotlin
private val pendingSrsUpdates = mutableListOf<WordSrsUpdate>()

fun onCardAnswered(wordId: String, answer: Answer) {
    pendingSrsUpdates.add(WordSrsUpdate(wordId, answer, computedNextDue, newInterval, newEase))
    // Update local UI state only — no network call here
}
```

**Step 2 — Flush on session close:**
When the session finishes, send all buffered updates in a single call alongside analytics sync.

```kotlin
suspend fun finishSession() {
    // Existing: sync analytics
    analyticsRepository.syncSession(sessionData)
    // New: flush all SRS updates in one batch
    wordRepository.batchUpdateSrs(pendingSrsUpdates)
    pendingSrsUpdates.clear()
}
```

**Step 3 — Backend endpoint:**
Use `POST /words/batch-update` (or `PATCH /words/batch`) if it exists, or send individual PATCHes sequentially/concurrently at session end. The key goal is that **no PATCH fires mid-session**.

**Step 4 — Crash / force-quit recovery:**
If the app crashes mid-session, SRS updates are lost. Persist `pendingSrsUpdates` to the local DB at each answer so they can be flushed on next launch.

## Acceptance Criteria

- **0** `PATCH /words/{id}` calls fire while the user is answering cards mid-session
- All SRS updates are sent when the session closes (alongside `POST /analytics/sync`)
- A 20-card session produces **1 batch request** (not 20 PATCH calls)
- If the app is killed mid-session, buffered updates are recovered and flushed on next launch
