---
name: wrapped-builder
description: Build Spotify Wrapped-style interactive story experiences from analytics data — animated stat reveals, swipeable story cards, personal learning journey narratives, and shareable summary screens
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["analytics-feature", "screen-patterns", "viewmodel-patterns", "design-system", "motion", "navigation-overlays"]
---

# Study Wrapped / Year-in-Review Builder

Build interactive, animated story experiences that summarize a user's learning journey.

## Story Architecture

```
WrappedViewModel (loads all data) → StoryPager (horizontal pager) → StoryCard composables (animated reveals)
```

### Data Loading

Load everything upfront, then paginate through story cards:

```kotlin
data class WrappedState(
    val data: UiState<WrappedData> = UiState.Loading,
    val currentPage: Int = 0,
)

data class WrappedData(
    val insights: StudyInsights,
    val difficultWords: List<WordDifficulty>,
    val comebackWords: List<ComebackWord>,
    val masteredWords: List<MasteredWord>,
    val languageStats: List<LanguagePairStats>,
    val monthlyStats: List<MonthlyStats>,
    val accuracyByHour: List<HourlyAccuracy>,
    val bestMonth: MonthlyStats?,
    val hardestWord: WordDifficulty?,
    val bestHour: HourlyAccuracy?,
)
```

### Story Card Sequence

1. **Opening**: "Your Learning Journey" + time period
2. **Total cards**: Big animated counter → "You reviewed X cards"
3. **Accuracy**: Circular progress animation → "X% accuracy"
4. **Study time**: "You spent X hours learning" with clock animation
5. **Best month**: "Your best month was [Month] — X cards, Y% accuracy"
6. **Languages**: Flags + stats for each language pair
7. **Hardest word**: "Your toughest word was [word] — you reviewed it X times"
8. **Comeback**: "But you conquered [word] after X attempts"
9. **Mastered**: "You mastered X words this [period]"
10. **Best time**: "You learn best at X:00 — Y% accuracy"
11. **Closing**: Summary card with share button

### Story Card Pattern

Each card is a full-screen composable with entrance animations:

```kotlin
@Composable
fun StoryCard(
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun CounterRevealCard(value: Long, label: String) {
    // Animate counter from 0 to value using animateIntAsState
    val animatedValue by animateIntAsState(
        targetValue = value.toInt(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            animatedValue.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
```

### Pager Implementation

Use `HorizontalPager` from Compose Foundation:

```kotlin
HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
) { page ->
    when (page) {
        0 -> OpeningCard()
        1 -> CounterRevealCard(data.insights.totalCardsReviewed, "cards reviewed")
        2 -> AccuracyCircleCard(data.insights.accuracyPercent)
        // ...
    }
}
// Page indicator dots at bottom
```

## Visual Design

- **Backgrounds**: Use rich gradient backgrounds — different color pair per card
- **Typography**: Display-size numbers, title-size labels
- **Animation**: Every stat has an entrance animation (counter, circle fill, slide-in)
- **Progress indicator**: Thin bar at top showing story progress (Instagram-style)
- **Navigation**: Tap left/right halves to go back/forward, swipe also works

## Color Palette per Card

```kotlin
val storyGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),  // Purple-blue
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),  // Pink
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),  // Sky blue
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),  // Green
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),  // Sunset
    // ...
)
```

## Sharing

The closing card has a "Share" button that exports a summary image card.
Delegate to `share-card-generator` agent for the export logic.

## Entry Points

- **Profile screen**: "View your [Year] Wrapped" card (only visible after sufficient data)
- **Insights screen**: "Year in Review" button in overview tab
- **Push notification**: Annual/quarterly trigger from backend scheduled job

## Minimum Data Requirements

Only show Wrapped if the user has meaningful data:
- At least 50 cards reviewed
- At least 5 sessions
- At least 3 different days studied

Show a "Keep studying to unlock your Wrapped!" teaser if thresholds not met.
