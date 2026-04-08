---
description: Offline-first sync contract for Lexicon — backend is source of truth, local DB is primary for reads, write-then-sync pattern
---

# Offline-First Sync

**IMPORTANT: Violating these rules silently breaks data consistency across devices.**

---

## The Contract

| Rule | Detail |
|------|--------|
| **Backend = source of truth** | On fresh sync, remote data wins. Never discard remote data. |
| **Local DB = primary for reads** | All UI reads from SQLDelight. Never read directly from network. |
| **Write-then-sync** | Write to local DB first → sync to backend in background. |
| **Sync on login** | `SyncRemoteToLocalUseCase` runs after every successful auth. |

---

## Data Flow

```
Write:  UI → ViewModel → UseCase → Repository → LocalDataSource (SQLDelight)
                                              ↘ RemoteDataSource (Ktor) [background]

Read:   UI ← ViewModel ← UseCase ← Repository ← LocalDataSource (SQLDelight) [only]
```

**NEVER read from `RemoteDataSource` to populate UI.** Remote calls are for sync only.

---

## Implementing a New Feature

### ✅ Correct Pattern

```kotlin
// Repository: write local first, sync in background
class WordRepositoryImpl(
    private val local: WordLocalDataSource,
    private val remote: WordRemoteDataSource,
    private val scope: CoroutineScope
) : WordRepository {

    override suspend fun saveWord(word: Word): Try<Unit> {
        // 1. Write to local immediately — UI updates instantly
        val result = local.insert(word.toEntity())
        // 2. Sync to backend in background — failure is non-blocking
        scope.launch { remote.createWord(word.toDto()).onFailure { /* queue retry */ } }
        return result
    }

    override fun observeWords(): Flow<List<Word>> =
        local.observeAll().map { it.map(WordEntity::toDomain) }  // local only
}

// ❌ Wrong: wait for remote before updating UI
override suspend fun saveWord(word: Word): Try<Unit> {
    remote.createWord(word.toDto()).getOrThrow()  // blocks UI, fails offline
    return local.insert(word.toEntity())
}

// ❌ Wrong: read from remote for UI
override fun observeWords(): Flow<List<Word>> =
    flow { emit(remote.fetchWords().getOrThrow()) }  // breaks offline
```

### Fresh Sync (on login)

`SyncRemoteToLocalUseCase` — always call after successful auth:

```kotlin
// Remote wins on fresh sync — replace local with remote state
override suspend fun syncRemoteToLocal(): Try<Unit> = Try {
    val remoteWords = remote.fetchWords().getOrThrow()
    local.replaceAll(remoteWords.map(WordDto::toEntity))  // atomic replace
}
```

**Do NOT merge on fresh sync.** Remote is authoritative. Merging introduces conflicts.

---

## New Data Types — Checklist

When adding a new entity (e.g., `Note`, `Collection`):

- [ ] SQLDelight schema + migration (`.sqm` file in order)
- [ ] `LocalDataSource` interface in `domain`, impl in `data`
- [ ] `RemoteDataSource` interface in `domain`, impl in `data` (Ktor)
- [ ] Repository: writes go local-first, reads are `Flow` from local
- [ ] Add entity to `SyncRemoteToLocalUseCase` — sync on login
- [ ] Backend sync failure must NOT block the local write `Try<Unit>`
- [ ] SQLDelight migration number must be sequential — check existing `.sqm` files

---

## Conflict Resolution Rules

| Scenario | Resolution |
|----------|-----------|
| Local write + remote write before sync | **Remote wins** on next sync (replaceAll pattern) |
| Local write, backend unreachable | Keep local, retry sync in background |
| Backend returns 409 Conflict | Accept remote state, discard local version |
| User deletes locally, remote still has it | Deletion propagates on next sync — do NOT restore from remote |

**Deletions are sticky.** If a user deletes something locally, do not restore it from a subsequent sync. Track deletions (tombstone pattern or soft-delete flag) so they survive a resync.

---

## Tags — Special Case

Tag operations touch two tables atomically: `TagEntity` + `WordTagEntity`.

```kotlin
// ✅ Atomic tag assignment — delete-all then insert-all in one transaction
override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try {
    db.transaction {
        wordTagQueries.deleteByWordId(wordId)
        tagIds.forEach { wordTagQueries.insert(wordId, it) }
    }
}

// ❌ Non-atomic — partial failure leaves inconsistent state
override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> {
    wordTagQueries.deleteByWordId(wordId)   // if crash here → tags lost
    tagIds.forEach { wordTagQueries.insert(wordId, it) }
    return Try.success(Unit)
}
```

Tag word list flows (`observeWords`) re-emit whenever `WordTagEntity` changes — keep tag operations transactional to avoid spurious intermediate emissions.

---

## Anti-Patterns

| ❌ Never | ✅ Instead |
|---------|-----------|
| Read from remote for UI | Read from `LocalDataSource` via `Flow` |
| Block write on remote success | Write local first, sync in background |
| Merge on fresh login sync | Replace local with remote (`replaceAll`) |
| Non-sequential migration numbers | Check last `.sqm` number, increment by 1 |
| Non-atomic multi-table writes | Wrap in `db.transaction { }` |
| Restore deleted items from sync | Track tombstones or skip deleted IDs |
