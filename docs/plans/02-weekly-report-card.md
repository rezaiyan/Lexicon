# Plan: Weekly Report Card in Insights

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Surface the already-computed weekly report in the Insights screen. All domain and formatting logic exists — it just isn't wired to any ViewModel.

## Context

- `GetWeeklyReportUseCase` — `domain/src/.../analytics/usecase/GetWeeklyReportUseCase.kt` — returns `Try<WeeklyReport>`
- `WeeklyReport` domain model — contains: `cardsReviewed`, `accuracyPercent`, `studyTimeMinutes`, `sessionCount`, `masteredThisWeek`, `bestDay`, `weekStart`, `weekEnd`, `previousWeekCards`
- `WeeklyReportFormatter` — `feature/study/src/.../formatter/WeeklyReportFormatter.kt` — formats to `WeeklyReportUiModel.Content` with all display strings
- `InsightsViewModel` — `feature/insights/src/.../InsightsViewModel.kt` — wire use case here
- `InsightsState` — add `weeklyReport: UiState<WeeklyReportUiModel>` field
- `InsightsScreen` — `feature/insights/src/.../ui/InsightsScreen.kt` — add weekly report section card
- `InsightsModule.kt` — inject `GetWeeklyReportUseCase` and `WeeklyReportFormatter`

## Implementation Tasks

- [ ] **T1** Add `weeklyReport: UiState<WeeklyReportUiModel>` to `InsightsState`
  - Import `WeeklyReportUiModel` from `feature.study.formatter` or move it to a shared location

- [ ] **T2** Inject `GetWeeklyReportUseCase` and `WeeklyReportFormatter` into `InsightsViewModel`
  - Add `loadWeeklyReport()` private function following existing `loadOverview()` pattern
  - Call from `loadAllData()`

- [ ] **T3** Add `WeeklyReportCard` composable to `InsightsScreen`
  - Show: week range, cards reviewed (with ▲/▼ change vs previous week), accuracy %, study time, sessions, mastered, best day
  - Position: after streak row, before accuracy trend chart
  - Loading state: shimmer placeholder

- [ ] **T4** Update `InsightsModule.kt` to provide `GetWeeklyReportUseCase` (check if already registered in `AppModule.kt`)

- [ ] **T5** Tests
  - `InsightsViewModelTest`: verify `weeklyReport` state transitions from Loading → Loaded
  - `InsightsViewModelTest`: verify error propagation when use case fails

## Files to Modify

| File | Change |
|------|--------|
| `feature/insights/src/.../InsightsViewModel.kt` | Add weekly report state + load function |
| `feature/insights/src/.../ui/InsightsScreen.kt` | Add `WeeklyReportCard` composable |
| `feature/insights/src/.../di/InsightsModule.kt` | Inject use case if needed |
| `composeApp/src/commonTest/.../InsightsViewModelTest.kt` | New test scenarios |

## Done: 0 / Left: 5
