# Plan: Post-Session Enhancements

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Enrich the review completion screen so users walk away knowing:
1. Which words they got wrong (missed words list)
2. Whether their streak moved
3. When the next session is due

Currently `ReviewState.Completed` only carries `knownCount` and `unknownCount`.

## Context

- `ReviewState.Completed` — `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModelState.kt`
- `ReviewCompletionContent` — `feature/study/src/commonMain/kotlin/feature/study/ui/review/ReviewCompletionContent.kt`
- `ReviewViewModel` — `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt`
  - Already injects `RecordStreakActivityUseCase` — streak is recorded but not surfaced
  - `ReviewState.Active` has `words: List<Word>` and `unknownCount` — we can track missed words
- `GetProfileStatsUseCase` — returns `ProfileStats(currentStreak, longestStreak)`
  - Already used by `InsightsViewModel`, available in DI
- `Word.nextReviewDate: LocalDate?` — next review date available on domain model

## Implementation Tasks

- [ ] **T1** Extend `ReviewState.Completed` to carry missed words + streak update
  - Add `missedWords: List<Word>`, `newStreak: Int?`, `nextDueCount: Int?` fields
  - In `ReviewViewModel`: collect missed words during active review (track words where result = unknown)
  - After session ends, fetch updated streak via `GetProfileStatsUseCase`, get next due count via `LoadReviewQueueUseCase`

- [ ] **T2** Update `ReviewCompletionContent` signature and UI
  - Accept `missedWords: List<Word>` and `currentStreak: Int?`
  - Show collapsible "Words to revisit" section below stats when `missedWords.isNotEmpty()`
  - Show streak badge if `currentStreak != null` and `currentStreak > 0`

- [ ] **T3** Wire changes in `ReviewScreen` — pass new fields from state to `ReviewCompletionContent`

- [ ] **T4** Tests
  - `ReviewViewModelTest`: verify `Completed` state contains correct missed words after rating unknown
  - `ReviewViewModelTest`: verify streak is fetched and included in `Completed` state
  - Fake `GetProfileStatsUseCase` in `:core:testing`

## Files to Modify

| File | Change |
|------|--------|
| `feature/study/src/.../ReviewViewModelState.kt` | Extend `ReviewState.Completed` |
| `feature/study/src/.../ReviewViewModel.kt` | Track missed words; fetch streak after completion |
| `feature/study/src/.../review/ReviewCompletionContent.kt` | Add missed words list + streak badge |
| `feature/study/src/.../review/ReviewScreen.kt` | Pass new fields from state |
| `composeApp/src/commonTest/.../ReviewViewModelTest.kt` | Add new scenario tests |

## Done: 0 / Left: 4
