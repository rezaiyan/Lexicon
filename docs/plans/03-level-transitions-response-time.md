# Plan: Level Transitions + Response Time Trend Charts

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Show users two powerful learning-progress signals that are fully computed but never displayed:
1. **Level transition history** — how words moved up/down SRS levels over time
2. **Response time trend** — whether the user is getting faster at recalling words

## Context

- `GetLevelTransitionsUseCase` — `domain/src/.../analytics/usecase/GetLevelTransitionsUseCase.kt` — returns `Try<List<LevelTransition>>`
- `LevelTransition` model — contains: `wordId`, `wordValue`, `fromLevel`, `toLevel`, `date`, `wasPromotion`
- `GetResponseTimeTrendUseCase` — `domain/src/.../analytics/usecase/GetResponseTimeTrendUseCase.kt` — returns `Try<List<ResponseTimeStat>>`
- `ResponseTimeStat` — check exact fields (likely: `date`, `avgResponseMs`)
- `InsightsViewModel` — wire both use cases here
- `InsightsState` — add two new fields
- `InsightsScreen` — add two new chart sections

## Implementation Tasks

- [ ] **T1** Read `GetResponseTimeTrendUseCase` and `LevelTransition`/`ResponseTimeStat` models to confirm field names

- [ ] **T2** Add `levelTransitions: UiState<List<LevelTransition>>` and `responseTimeTrend: UiState<List<ResponseTimeStat>>` to `InsightsState`

- [ ] **T3** Inject both use cases into `InsightsViewModel`, add `loadLevelTransitions()` and `loadResponseTimeTrend()` private functions, call from `loadAllData()`

- [ ] **T4** `LevelTransitionChart` composable
  - Compact list/timeline showing recent promotions and demotions
  - Green pill for promotion (level up), red pill for demotion (level down)
  - Show word value + old level → new level + date

- [ ] **T5** `ResponseTimeTrendChart` composable
  - Line chart of avg response time per day over 30 days
  - Lower = better; show trend direction label ("Getting faster!" / "Slowing down")

- [ ] **T6** Add both sections to `InsightsScreen` below existing charts

- [ ] **T7** Update `InsightsModule.kt` / `AppModule.kt` to register new use cases if needed

- [ ] **T8** Tests
  - `InsightsViewModelTest`: verify both new states load correctly
  - `InsightsViewModelTest`: verify partial failure (one use case fails) doesn't break others

## Files to Modify

| File | Change |
|------|--------|
| `feature/insights/src/.../InsightsViewModel.kt` | Add 2 new states + load functions |
| `feature/insights/src/.../ui/InsightsScreen.kt` | Add 2 new chart composables |
| `feature/insights/src/.../di/InsightsModule.kt` | Register new use cases |
| `composeApp/src/commonTest/.../InsightsViewModelTest.kt` | New test scenarios |

## Done: 0 / Left: 8
