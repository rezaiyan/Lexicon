# Plan: Insights Actionable CTAs

**Status:** COMPLETE
**Type:** Feature
**Worktree:** No
**Approved:** Yes

## Goal

Turn the Insights screen from a passive dashboard into an action driver. Two high-value CTAs:
1. **"Study these now"** button on the Difficult Words section → launches a review session filtered to those words
2. **"Set reminder"** or **"Study at this time"** button on Best Study Time card → opens notification settings or schedules a notification for that hour

## Context

- `InsightsScreen` — `feature/insights/src/.../ui/InsightsScreen.kt` — currently shows charts, no CTAs
- `InsightsViewModel` — `feature/insights/src/.../InsightsViewModel.kt` — add `InsightsEffect` sealed class (currently `Nothing`)
- Difficult words: `WordDifficulty(wordId, wordValue, errorRate, reviewCount)` — need word IDs to launch filtered session
- `ReviewType` — `feature/study/src/.../model/ReviewType.kt` — check if filtered/custom session type exists
- Navigation: need to navigate from Insights to Review with a word subset — check `InsightsRoute` and nav graph

## Implementation Tasks

- [x] **T1** Introduce `InsightsEffect` sealed class (currently `Nothing`)
  - `data class NavigateToReviewWithWords(val wordIds: List<Long>): InsightsEffect`
  - `data object NavigateToNotificationSettings: InsightsEffect`
  - Change `InsightsViewModel` type parameter from `Nothing` to `InsightsEffect`

- [x] **T2** Add `studyDifficultWords()` function to `InsightsViewModel`
  - Extract word IDs from current `difficultWords` state
  - Emit `NavigateToReviewWithWords(wordIds)`

- [x] **T3** Check if `LoadReviewQueueUseCase` supports filtering by specific word IDs
  - If not, add filtered launch support or use existing `ReviewSource` enum

- [x] **T4** Add `setReminderForBestTime()` to `InsightsViewModel`
  - Extract hour from `bestStudyTime` state
  - Emit `NavigateToNotificationSettings` (for now, navigate to settings; future: schedule directly)

- [x] **T5** Update `InsightsScreen` to handle effects and add CTA buttons
  - "Study these words" button below difficult words list
  - "Study at best time" chip/button below best study time display

- [x] **T6** Tests
  - `InsightsViewModelTest`: verify `studyDifficultWords()` emits correct effect with correct IDs
  - `InsightsViewModelTest`: verify `setReminderForBestTime()` emits correct effect

## Files to Modify

| File | Change |
|------|--------|
| `feature/insights/src/.../InsightsViewModel.kt` | Add effect type + two new functions |
| `feature/insights/src/.../ui/InsightsScreen.kt` | Add CTA buttons + effect handler |
| `composeApp/src/commonTest/.../InsightsViewModelTest.kt` | New effect tests |

## Done: 6 / Left: 0
