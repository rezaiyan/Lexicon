# Plan: Day-of-Week Accuracy Chart

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Show users which days of the week they study most accurately. This helps them identify their best study days and schedule sessions accordingly. The data is in the analytics DB — need a use case + chart.

## Context

- `IAnalyticsStatsRepository` — check if `getAccuracyByDayOfWeek()` exists
- `DailyStudyStats` — existing model, has `date: String` — can aggregate by day-of-week from existing data
- `GetAccuracyTrendUseCase` — already loads 30-day trend; can derive day-of-week from it client-side
- Alternative: add `GetAccuracyByDayOfWeekUseCase` to domain
- `InsightsViewModel` — add new state field
- `InsightsScreen` — add bar chart section

## Implementation Tasks

- [ ] **T1** Decide: derive from existing `accuracyTrend` data client-side OR add new use case
  - Preferred: derive client-side in ViewModel from existing 30-day data (avoids new DB query)
  - Map `DailyStudyStats.date` → day of week → aggregate accuracy per day

- [ ] **T2** Add `accuracyByDayOfWeek: List<DayOfWeekAccuracy>` computed field to `InsightsState`
  - `DayOfWeekAccuracy(dayName: String, accuracyPercent: Float, reviewCount: Int)`
  - Compute in `InsightsViewModel` when `accuracyTrend` loads

- [ ] **T3** `DayOfWeekAccuracyChart` composable in `InsightsScreen`
  - 7 bars (Mon–Sun), height proportional to accuracy %
  - Color-coded: green ≥ 80%, yellow 60–80%, red < 60%
  - Highlight current day of week
  - Show review count below each bar as context

- [ ] **T4** Add section to `InsightsScreen` between heatmap and best study time

- [ ] **T5** Tests
  - Unit test: `computeDayOfWeekAccuracy()` correctly aggregates 30-day data
  - Test edge case: no data for certain days → show 0 / grey bar

## Files to Modify

| File | Change |
|------|--------|
| `feature/insights/src/.../InsightsViewModel.kt` | Add computation + new state field |
| `feature/insights/src/.../ui/InsightsScreen.kt` | Add chart composable |
| `composeApp/src/commonTest/.../InsightsViewModelTest.kt` | Aggregation correctness tests |

## Done: 0 / Left: 5
