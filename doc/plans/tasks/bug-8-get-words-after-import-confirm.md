# BUG-8 — `GET /words` Fires Immediately After AI Import Confirm

**Priority:** P1
**Status:** Open

## Observed Behaviour

3ms after a successful `POST /words` batch import, a `GET /words` fires redundantly:

```
→ POST  https://api.vokab.app/words   (10:14:58.689)   ← batch import (23KB, 200 OK)
← 200   /words (133ms)

→ GET   https://api.vokab.app/words   (10:14:58.825)   ← unnecessary re-sync
← 200   /words (89ms)
```

Also observed on text import path (`POST /onboarding/preferences` → `POST /words` → `GET /words`).

## Root Cause

After the batch `POST /words` succeeds, the repository calls `syncWithRemote()` to refresh the local word cache. But the `POST /words` **response already contains the created words** — there is no new information to fetch from the server.

The full word list re-fetch is wasted bandwidth immediately after a write that returned all the data needed.

## Fix

After a successful `POST /words` batch, update the local SQLDelight database **directly from the POST response body** instead of triggering `syncWithRemote()`.

```kotlin
// ImportWordRepository (or WordRepositoryImpl)
suspend fun importWords(words: List<Word>): Try<List<Word>> {
    return apiClient.postNotNull<List<WordDto>>(path = "/words", body = words.toDto())
        .map { createdWords ->
            // Write the returned words directly to the local DB
            localDataSource.upsertWords(createdWords.map { it.toDomain() })
            // Do NOT call syncWithRemote() here
            createdWords.map { it.toDomain() }
        }
}
```

If the `POST /words` response does not include the full word objects, request that the backend return them (standard REST practice for POST batch create). If that is not feasible short-term, use the BUG-2 `lastSyncedAt` timestamp to suppress the immediate re-fetch (the import counts as a sync).

## Acceptance Criteria

- After AI image import confirm: **1** `POST /words`, **0** `GET /words`
- After text import confirm: **1** `POST /words`, **0** `GET /words`
- The local word list is correctly updated after import (words appear immediately)
- The fix works for both single-word and batch imports
