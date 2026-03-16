---
name: analytics-insight-builder
description: Build analytics-powered insight features (charts, stat cards, coaching tips, milestone celebrations) on top of Lexicon's 16 backend analytics endpoints, following the backend-only read pattern
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["analytics-feature", "screen-patterns", "viewmodel-patterns", "design-system", "testing-patterns"]
---

# Analytics Insight Builder

Build insight/analytics features for Lexicon that read study data from the backend and present it to users as motivational, actionable UI.

## Before Writing Code

1. **Read the analytics skill** — load `analytics-feature` skill to understand all available endpoints and patterns
2. **Check what exists** — read `feature/insights/` to see the current InsightsViewModel and InsightsScreen
3. **Check backend endpoints** — read `AnalyticsController.kt` in the backend to see all available data
4. **Identify if this extends InsightsScreen** (new tab/card) or needs a **new feature module**
5. **Present plan** to user and wait for approval

## Implementation Flow

### For extending InsightsScreen:
1. Add new `UiState<T>` field to `InsightsState`
2. Add use case (or reuse existing) in `domain/analytics/usecase/`
3. Add loader method in `InsightsViewModel`
4. Add UI composable in `feature/insights/ui/`
5. Register any new use cases in `AnalyticsModule.kt`
6. Delegate to `test-writer` agent for InsightsViewModel test updates

### For new standalone feature:
1. Create feature module under `feature/` with `lexicon.kmp.feature-ui` plugin
2. Create ViewModel extending `BaseViewModel<State, Nothing>`
3. Create Screen with `koinViewModel` + `viewModel.state()` + `LexiconColumn`
4. Create `@Serializable` route + `NavGraphBuilder` extension
5. Create DI module, register in `PresentationModule.kt`
6. Add navigation entry point (from InsightsScreen, StudyScreen, or ProfileScreen)
7. Wire into `settings.gradle.kts`, `composeApp/build.gradle.kts`, `presentation/build.gradle.kts`
8. Delegate to `test-writer` agent

## Data Visualization Guidelines

- **Stat cards**: Use the `StatCard` pattern from `InsightsScreen.kt` — Icon + title + value + subtitle
- **Progress bars**: `LinearProgressIndicator` with `progress = { percent / 100f }`
- **Lists**: `LazyColumn` with `Card` items for word lists (difficult words, mastered words)
- **Theme**: Use `MaterialTheme.typography` and `MaterialTheme.colorScheme`, spacing from `Theme.spacing.*`
- **Empty states**: Always handle "no data yet" with encouraging messages and CTA
- **Loading**: Use `UiState.Loading` → `LoadingScreen(message = "...")` pattern
- **Errors**: Friendly messages — analytics errors should never feel alarming

## Key Principles

1. **Analytics never block study** — if backend is down, show empty state, never crash
2. **Data motivates, never shames** — frame everything positively ("You're improving!" not "You failed 40%")
3. **Actionable over informational** — every insight should suggest a next step
4. **Design system first** — check existing components before creating new ones
5. **Backend computes, client displays** — never do heavy aggregation on client
