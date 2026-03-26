# BUG-6 — All 6 Insights Endpoints Fire Twice on Screen Open

**Priority:** P1
**Status:** Open

## Observed Behaviour

Every analytics endpoint fires twice (12 total requests) within 70ms of opening the Insights screen:

```
First batch  (09:53:44.562–.596):
→ GET /analytics/insights
→ GET /analytics/daily-stats
→ GET /analytics/difficult-words
→ GET /analytics/accuracy-by-level
→ GET /analytics/heatmap
→ GET /analytics/accuracy-by-hour

Second batch (09:53:44.632–.643):   ← 70ms later, identical
→ GET /analytics/insights
→ GET /analytics/daily-stats
→ GET /analytics/difficult-words
→ GET /analytics/accuracy-by-level
→ GET /analytics/heatmap
→ GET /analytics/accuracy-by-hour
```

## Root Cause

Same double-trigger pattern as BUG-4. Both `init` and the screen's `LaunchedEffect` call `loadAllData()`:

**Trigger 1 — ViewModel init:**
```kotlin
// InsightsViewModel.kt:65-67
init {
    loadAllData()   // ← fires all 6 requests immediately on construction
}
```

**Trigger 2 — Screen LaunchedEffect:**
```kotlin
// InsightsScreen.kt:104-106
LaunchedEffect(Unit) {
    viewModel.refresh()   // ← calls loadAllData() again on first composition
}
```

**Files:**
- `feature/insights/src/commonMain/kotlin/feature/insights/InsightsViewModel.kt:65-67`
- `feature/insights/src/commonMain/kotlin/feature/insights/ui/InsightsScreen.kt:104-106`

## Fix

Remove `loadAllData()` from `InsightsViewModel.init`. The `LaunchedEffect(Unit)` in the screen already drives the initial load on every visit.

```kotlin
// InsightsViewModel.kt — BEFORE
init {
    loadAllData()   // ← remove this
}

// InsightsViewModel.kt — AFTER
init {
    // Initial load driven by LaunchedEffect in InsightsScreen
}
```

The `refresh()` function and `loadAllData()` remain unchanged — they are still called by the screen's `LaunchedEffect` and by pull-to-refresh.

## Acceptance Criteria

- Each of the 6 analytics endpoints fires **exactly once** when the Insights screen opens
- Pull-to-refresh still triggers a full reload (all 6 endpoints, once each)
- Re-navigating to the screen triggers a fresh load via `LaunchedEffect(Unit)`
