# Plan: "Nothing Due" Empty State with Next-Review Countdown

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

When no words are due for review, show a motivating empty state that tells users exactly when their next session starts — instead of leaving them staring at a blank screen.

## Context

- `ReviewState.Empty` — exists in `ReviewViewModelState.kt` but currently shows a generic "nothing to review" message
- `ReviewScreen` — `feature/study/src/.../ui/review/ReviewScreen.kt` — renders `ReviewState.Empty`
- `LoadReviewQueueUseCase` — can be called with a future date to find next due words
- `Word.nextReviewDate: LocalDate?` — the domain model has next review date
- Need to find the minimum `nextReviewDate` across all words that are NOT currently due
- `GetNextDueWordDateUseCase` or equivalent — check if it exists; if not, query via `IWordRepository`

## Implementation Tasks

- [ ] **T1** Check if `GetNextDueWordDateUseCase` or `IWordRepository.getNextDueDate()` exists
  - If not, add `suspend fun getNextDueAt(): Try<LocalDate?>` to `IWordRepository` and impl
  - Add simple `GetNextDueDateUseCase` if needed (NoParamUseCase)

- [ ] **T2** Extend `ReviewState.Empty` to carry `nextDueDate: LocalDate?`
  - When ViewModel transitions to Empty, fetch next due date and populate

- [ ] **T3** Update `ReviewScreen` empty state rendering
  - If `nextDueDate != null`: show countdown "Next words due in X hours Y minutes" with a clock icon
  - If `nextDueDate == null`: show "All words mastered! 🎉"
  - Add "Explore Words" button that navigates to word list

- [ ] **T4** Tests
  - `ReviewViewModelTest`: verify Empty state includes correct `nextDueDate`
  - Mock repository returning future date

## Files to Modify

| File | Change |
|------|--------|
| `domain/src/.../word/repository/IWordRepository.kt` | Add `getNextDueAt()` if missing |
| `data/src/.../word/WordRepositoryImpl.kt` | Implement `getNextDueAt()` |
| `feature/study/src/.../ReviewViewModelState.kt` | Extend `ReviewState.Empty` |
| `feature/study/src/.../ReviewViewModel.kt` | Fetch next due date on empty queue |
| `feature/study/src/.../ui/review/ReviewScreen.kt` | Update empty state UI |
| `composeApp/src/commonTest/.../ReviewViewModelTest.kt` | New test scenarios |

## Done: 0 / Left: 4
