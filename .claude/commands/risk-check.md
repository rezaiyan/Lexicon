---
description: Cross-check staged/recent changes against critical-risks.md — catches dangerous patterns before they ship
allowed-tools: ["Bash", "Read", "Glob", "Grep"]
---

Check all staged and recent changes against Lexicon's known critical risks.

## Step 1: Get the Changes

```bash
git diff HEAD~1..HEAD --name-only
git diff --cached --name-only
git diff --name-only
```

Read all changed files.

## Step 2: Classify Changed Files

For each changed file, identify which risk categories apply:

| Changed file contains... | Read risk section |
|---|---|
| `ReviewWordUseCase`, SRS algorithm, `level`, `interval`, `easeFactor` | **Risk 1: SRS Algorithm** |
| `AnalyticsRecorder`, `recordReview`, `startSession`, `endSession`, `sessionId` | **Risk 2: Analytics Session Lifecycle** |
| `ReviewViewModel` | **Risk 2: Analytics** + **Risk 1: SRS** |
| `TokenRefreshManager`, `AuthInterceptor`, token refresh | **Risk 3: Token Refresh Race Conditions** |
| `AppNavigationViewModel`, `AppUiState`, onboarding | **Risk 4: Navigation State Machine** |
| `syncRemoteToLocal`, `deleteWord`, `batchUpdate` | **Risk 5: Word Sync & Deletion** |
| `TagLocalDataSource`, `deleteTag`, `setWordTags`, `syncTagsFromRemote` | **Risk 9: Tag Cascades & Reactivity** |
| `SettingsLocalDataSourceImpl`, Boolean/Long/Int settings | **Risk 6: Settings Persistence** |
| `IOSKeychainSecureStorage`, `CFDataRef`, Keychain | **Risk 7: iOS Keychain** |
| `Try<T>`, `.map {}`, `.flatMap {}`, `CancellationException` | **Risk 8: Try<T> Error Handling** |
| Remote batch operations, `forEach { remoteCall() }` | **Risk 10: Batch Remote Operations** |

## Step 3: Check Each Applicable Risk

For each matched risk, check the specific invariants against the diff:

### Risk 1 (SRS Algorithm)
- [ ] `repetitions` resets to 0 on level advance
- [ ] Ease factor bounded: min 1.3, max 2.5
- [ ] `interval` can never be 0 (especially at level 6)
- [ ] `LEVEL_INTERVALS` map not modified

### Risk 2 (Analytics)
- [ ] `startSession()` is awaited before any `recordReviewEvent()` calls
- [ ] `recordReviewEvent()` is inside `if (currentSessionId != null)` guard
- [ ] `endSession()` is called on session completion AND ViewModel `onCleared()`
- [ ] No removal of `Mutex` from `AnalyticsRecorderImpl.sessions`
- [ ] `newLevel` in analytics matches what `ReviewWordUseCase` computes

### Risk 3 (Token Refresh)
- [ ] Double-check after Mutex acquire is still present
- [ ] `AuthenticationException` vs transient error distinction preserved
- [ ] Proactive refresh not made blocking

### Risk 4 (Navigation)
- [ ] `AppUiState.Ready` only reached after both auth AND onboarding checks
- [ ] `hasCompletedOnboarding()` edge case (reinstall + surviving Keychain) handled
- [ ] `getTotalCount()` fallback to 0 → Onboarding preserved (safe fallback)

### Risk 5 (Sync/Delete)
- [ ] Delete order: backend first, then local
- [ ] Batch updates tolerate partial failure
- [ ] Sync conflict resolver is deterministic

### Risk 9 (Tags)
- [ ] `deleteTag()` transaction: `deleteWordTagsForTag()` BEFORE `deleteTag()`
- [ ] `setWordTags()` is atomic (delete-all + insert in one transaction)
- [ ] No `addTagToWord()` loops in batch operations (use `setWordTags()` instead)
- [ ] `countWordTags()` combine trigger not removed from word flows

### Risk 10 (Batch Remote Ops)
- [ ] No `forEach { remoteDataSource.singleOp(id).getOrThrow() }` loops
- [ ] Batch mutations use a dedicated batch endpoint
- [ ] Client-side batch writes in one `queries.transaction { }`, not N separate ones

## Step 4: Report

Format findings as:

```
## Risk Check Results

### Critical (must fix before merge)
- [finding + file + line]

### High (fix soon)
- [finding + file + line]

### Clear
- Risk 1 (SRS): OK
- Risk 2 (Analytics): OK
- ...
```

If no changes touch any risk area: "No risk areas touched by these changes."

## What This Is Not

This command checks known critical risks. It does NOT replace:
- `/review` — architecture pattern compliance check
- `architecture-reviewer` agent — module boundary check
- Full test suite — `./gradlew composeApp:cleanAllTests composeApp:allTests`

Run all three before merging significant changes.
