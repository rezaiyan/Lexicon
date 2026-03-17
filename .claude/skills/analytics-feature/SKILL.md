---
name: analytics-feature
description: Build features on top of Lexicon's study analytics infrastructure — insights screens, motivational features, data visualizations, share cards, and coaching using the 16 backend analytics endpoints
argument-hint: "<feature-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Analytics Feature Patterns

Use this skill when building any feature that consumes study analytics data (insights, milestones, coaching, share cards, charts, reports).

## Analytics Data Infrastructure

All analytics data lives on the **backend only** (PostgreSQL). The client buffers review events in memory during a session and POSTs them on session end. All reads come from backend API calls.

### Available Backend Endpoints

```
POST /api/v1/analytics/sync              → Bulk upload sessions + events
GET  /api/v1/analytics/insights          → Aggregate overview (cards, accuracy, time, sessions, mastered)
GET  /api/v1/analytics/daily-stats       → Daily breakdown (date range)
GET  /api/v1/analytics/difficult-words   → Highest error rate words
GET  /api/v1/analytics/most-reviewed     → Most frequently reviewed
GET  /api/v1/analytics/accuracy-by-level → Per-SRS-level accuracy
GET  /api/v1/analytics/accuracy-by-hour  → Hour-of-day accuracy
GET  /api/v1/analytics/accuracy-by-day-of-week → Day-of-week accuracy
GET  /api/v1/analytics/sessions          → Recent session history
GET  /api/v1/analytics/heatmap           → Activity calendar data
GET  /api/v1/analytics/level-transitions → Level change matrix
GET  /api/v1/analytics/words-mastered    → Recently mastered words
GET  /api/v1/analytics/language-stats    → Per-language-pair breakdown
GET  /api/v1/analytics/monthly-stats     → Monthly rollup
GET  /api/v1/analytics/response-time-trend → Weekly response time trend
GET  /api/v1/analytics/comeback-words    → Words recovered from big drops
```

### Client Data Flow

```
ReviewViewModel → buffers events in memory → endSession() → POST /analytics/sync → backend stores
InsightsViewModel → GET /analytics/* → maps response → domain model → UI state
```

### Existing Client Classes

```
domain/analytics/model/          → 15 domain models (StudyInsights, WordDifficulty, AccuracyByLevel, etc.)
domain/analytics/repository/     → IAnalyticsRecorder (write), IAnalyticsRepository (read)
domain/analytics/usecase/        → 9 use cases (Start/End/RecordSession, GetInsights, GetDifficultWords, etc.)
data/analytics/remote/           → IAnalyticsRemoteDataSource (13 methods), AnalyticsRemoteDataSource
data/analytics/remote/model/     → Serializable DTOs matching backend responses
data/analytics/repository/       → AnalyticsRecorderImpl (in-memory buffer), AnalyticsRepositoryImpl (backend reads)
feature/insights/                → InsightsViewModel, InsightsScreen, InsightsRoute, InsightsModule
composeApp/.../di/AnalyticsModule.kt → DI for all analytics components
```

## Adding a New Analytics Feature

### Step 1: Check if the backend endpoint already exists

All 16 endpoints are already deployed. If your feature needs data that an existing endpoint provides, skip to Step 3.

If you need a **new aggregation** (e.g., "words learned per week by language"):
1. Add a new `@Query` to `ReviewEventRepository.kt` or `StudySessionRepository.kt` on the backend
2. Add a method to `AnalyticsService.kt`
3. Add an endpoint to `AnalyticsController.kt`
4. Add the response DTO to backend `AnalyticsDto.kt`
5. Add matching `@Serializable` response class to client `AnalyticsRemoteModels.kt`
6. Add method to `IAnalyticsRemoteDataSource` + `AnalyticsRemoteDataSource`

### Step 2: Add domain model + use case (if new data shape)

```kotlin
// domain/analytics/model/NewMetric.kt
data class NewMetric(val value: Int, val trend: Double)

// domain/analytics/usecase/GetNewMetricUseCase.kt
class GetNewMetricUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : NoParamUseCase<NewMetric> {
    override suspend fun invoke(params: Unit): Try<NewMetric> =
        analyticsRepository.getNewMetric()
}
```

If reusing existing endpoints, use existing use cases directly.

### Step 3: Add to the Insights screen OR create a new screen

**Option A: Extend InsightsScreen** (for new data cards/tabs)

Add a new field to `InsightsState`:
```kotlin
data class InsightsState(
    // ... existing fields ...
    val newMetric: UiState<NewMetric> = UiState.Loading,
)
```

Add a loader in `InsightsViewModel`:
```kotlin
private fun loadNewMetric() {
    viewModelScope.launch {
        updateState { copy(newMetric = UiState.Loading) }
        getNewMetricUseCase(Unit).reduce(
            onSuccess = { copy(newMetric = UiState.Loaded(it)) },
            onFailure = { copy(newMetric = UiState.Error(it.message ?: "Failed")) },
        )
    }
}
```

**Option B: New feature module** (for standalone screens like Wrapped, Share Cards)

Follow the `:feature:insights` module structure:
```
feature/newfeature/
├── build.gradle.kts                     # id("lexicon.kmp.feature-ui")
├── src/commonMain/kotlin/feature/newfeature/
│   ├── NewFeatureViewModel.kt           # BaseViewModel<State, Nothing>
│   ├── di/NewFeatureModule.kt           # viewModel { } + register in PresentationModule
│   ├── navigation/NewFeatureRoute.kt    # @Serializable route + NavGraphBuilder extension
│   └── ui/NewFeatureScreen.kt           # koinViewModel + state() + LexiconColumn
```

### Step 4: Register DI

New use cases → `AnalyticsModule.kt`:
```kotlin
singleOf(::GetNewMetricUseCase)
```

New ViewModel → feature's `di/` module + include in `PresentationModule.kt`.

## Data Visualization Patterns

### Visual Style for Analytics

Analytics screens must feel **calm and informative** — data should be easy to scan without visual noise:
- Use generous whitespace between stat cards and sections
- Neutral surface colors as base — accent color only for key metrics or positive trends
- Numbers are the hero content — use `headlineMedium` Bold for primary values, `bodySmall` for labels
- Green (`Theme.semanticColors.success`) for positive trends, `onSurfaceVariant` for neutral
- Cards with `Theme.elevation.low` (1dp) subtle shadow — data-heavy screens stay calm with minimal elevation
- Maximum 4 stat cards visible at once — avoid overwhelming the user

### Stat Cards

Use the `StatCard` composable from InsightsScreen as the base pattern:
```kotlin
// Clean stat card — label on top, large value, optional trend subtitle
Card(
    shape = RoundedCornerShape(Theme.shapes.medium),  // 12dp
    elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
) {
    Column(Modifier.padding(Theme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Theme.dimensions.iconMedium))
            Spacer(Modifier.width(Theme.spacing.xs))
            Text(title, style = MaterialTheme.typography.labelMedium, color = onSurfaceVariant)
        }
        Spacer(Modifier.height(Theme.spacing.xs))
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Spacer(Modifier.height(Theme.spacing.xxs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = semanticColors.success)
        }
    }
}
```

### Stat Card Grid

Arrange stat cards in a 2-column grid with consistent spacing:
```kotlin
// 2-column grid with 16dp gap
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
    StatCard(modifier = Modifier.weight(1f), ...)
    StatCard(modifier = Modifier.weight(1f), ...)
}
Spacer(Modifier.height(Theme.spacing.md))
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
    StatCard(modifier = Modifier.weight(1f), ...)
    StatCard(modifier = Modifier.weight(1f), ...)
}
```

### Progress Bars (accuracy, completion)

```kotlin
// Rounded progress bar matching card radius
LinearProgressIndicator(
    progress = { (accuracyPercent / 100.0).toFloat() },
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Theme.shapes.small)),
    trackColor = MaterialTheme.colorScheme.surfaceVariant,
)
```

### Charts

For richer charts (line, bar, heatmap), evaluate:
1. **Compose Canvas** — custom drawing for simple charts (bar charts, sparklines)
2. **Vico** (`com.patrykandpatrick.vico`) — KMP charting library if complex charts needed
3. **Custom Compose** — heatmap grids, streak calendars using `LazyVerticalGrid`

Chart visual rules:
- Minimal gridlines — use light `outlineVariant` color, no heavy borders
- Data points and lines in `primary` color — keep charts monochromatic unless comparing categories
- Axis labels in `labelSmall`, `onSurfaceVariant` color
- Generous padding around charts: `Theme.spacing.md` (16dp) inside the card
- Wrap charts in cards with same 12dp radius as all other cards

### Shareable Image Cards

```kotlin
// Render Compose content to Bitmap for sharing
val imageBitmap = remember { ImageBitmap(width, height) }
Canvas(imageBitmap) { /* draw card content */ }
// Share via platform share sheet
```

## Motivational Feature Patterns

### Milestone Detection

Check milestones after each session end:
```kotlin
class CheckMilestonesUseCase(
    private val repository: IAnalyticsRepository,
) : NoParamUseCase<List<Milestone>> {
    override suspend fun invoke(params: Unit): Try<List<Milestone>> =
        repository.getStudyInsights().map { insights ->
            buildList {
                if (insights.totalCardsReviewed >= 100 && !wasPreviouslyAchieved("100_cards"))
                    add(Milestone.CardsReviewed100)
                if (insights.wordsMasteredCount >= 10 && !wasPreviouslyAchieved("10_mastered"))
                    add(Milestone.WordsMastered10)
                // ...
            }
        }
}
```

### Weekly Report Push Notification

Compute on backend via scheduled job → push notification with summary data.
Client receives notification → deep links to InsightsScreen.

### Coaching Tips

Derive tips from pattern analysis:
```kotlin
class GetStudyTipsUseCase(
    private val repository: IAnalyticsRepository,
) : NoParamUseCase<List<StudyTip>> {
    override suspend fun invoke(params: Unit): Try<List<StudyTip>> {
        val insights = repository.getStudyInsights().getOrNull() ?: return Try.success(emptyList())
        val byLevel = repository.getAccuracyByLevel().getOrNull().orEmpty()
        val byHour = repository.getAccuracyByHourOfDay().getOrNull().orEmpty()
        return Try.success(deriveTips(insights, byLevel, byHour))
    }
}
```

## Feature Proposals Reference

See `doc/analytics-feature-proposals.md` for the full list of 10 proposed features with priority matrix and rollout phases.

## Error Handling

All analytics features must handle the offline case gracefully:
```kotlin
state.overview
    .onLoading { LoadingScreen(message = "Loading insights...") }
    .onError { msg, _ ->
        // Show friendly message — analytics are "nice to have"
        ErrorScreen(
            message = "Connect to the internet to see your study insights",
            retryLabel = "Try Again",
            onRetry = { viewModel.refresh() },
        )
    }
    .onLoaded { insights -> /* render */ }
```

Analytics features should **never block** the core study flow. If a GET fails, show empty state with retry. If a POST fails, silently drop (events are best-effort).
