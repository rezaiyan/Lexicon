# Plan: Day-of-Week Accuracy Chart

**Status:** COMPLETE
**Type:** Feature
**Worktree:** No
**Approved:** Yes

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

- [x] **T1** Decide: derive from existing `accuracyTrend` data client-side OR add new use case
  - Preferred: derive client-side in ViewModel from existing 30-day data (avoids new DB query)
  - Map `DailyStudyStats.date` → day of week → aggregate accuracy per day

- [x] **T2** Add `accuracyByDayOfWeek: List<DayOfWeekAccuracy>` computed field to `InsightsState`
  - Uses existing `DayOfWeekAccuracy(dayOfWeek: Int, totalReviews: Long, correctCount: Long, accuracyPercent: Double)`
  - Computed in `InsightsViewModel.loadAccuracyTrend()` via `computeDayOfWeekAccuracy()`

- [x] **T3** `DayOfWeekAccuracyChart` composable in `InsightsScreen`
  - 7 bars (Mon–Sun), height proportional to accuracy %
  - Color-coded: green ≥ 80%, amber 60–80%, red < 60%, grey for no data
  - Highlights current day of week with full opacity + amber label

- [x] **T4** Add section to `InsightsScreen` TrendsTab between AccuracyByLevelCard and LevelTransitionsCard

- [x] **T5** Tests
  - Unit test: `accuracyByDayOfWeek has 7 entries after refresh`
  - Unit test: aggregates Sunday (2026-03-01) correctly from default stats (80% accuracy)
  - Unit test: zero reviews for days with no stats
  - Unit test: all zeros when trend data is empty list
  - Unit test: remains empty when trend load fails

## Files to Modify

| File | Change |
|------|--------|
| `feature/insights/src/.../InsightsViewModel.kt` | Add computation + new state field |
| `feature/insights/src/.../ui/InsightsScreen.kt` | Add chart composable |
| `composeApp/src/commonTest/.../InsightsViewModelTest.kt` | Aggregation correctness tests |

## Done: 5 / Left: 0
