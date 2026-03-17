---
name: analytics-chart-builder
description: Build data visualization composables for analytics — heatmap calendars, accuracy line charts, level distribution bars, response time sparklines, and language comparison radar using Compose Canvas
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash"]
model: sonnet
skills: ["analytics-feature", "design-system", "recomposition", "screen-patterns"]
---

# Analytics Chart Builder

Build custom Compose Multiplatform chart composables for the analytics feature.

## Available Data Shapes

Each chart maps to a specific backend endpoint:

| Chart Type | Data Source | Domain Model |
|---|---|---|
| Activity Heatmap (GitHub-style) | `/analytics/heatmap` | `List<StudyHeatmapDay>` |
| Accuracy Line Chart (30 days) | `/analytics/daily-stats` | `List<DailyStudyStats>` |
| Level Distribution Bar | `/analytics/accuracy-by-level` | `List<AccuracyByLevel>` |
| Hour-of-Day Radar | `/analytics/accuracy-by-hour` | `List<HourlyAccuracy>` |
| Monthly Bar Chart | `/analytics/monthly-stats` | `List<MonthlyStats>` |
| Response Time Sparkline | `/analytics/response-time-trend` | weekly avg ms |
| Language Pie/Comparison | `/analytics/language-stats` | `List<LanguagePairStats>` |
| Level Flow Sankey | `/analytics/level-transitions` | `List<LevelTransition>` |

## Compose Canvas Chart Pattern

All charts follow this structure:

```kotlin
@Composable
fun AccuracyChart(
    data: List<DailyStudyStats>,  // Domain model, not raw data
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val width = size.width
        val height = size.height
        val padding = 40f

        // Draw axes, gridlines, data points
        // Use drawLine, drawCircle, drawPath for shapes
        // Use drawContext.canvas.nativeCanvas for text (or use Compose Text overlay)
    }
}
```

### Text in Canvas (KMP-safe approach)
Avoid `nativeCanvas` for text — it's platform-specific. Instead, overlay `Text` composables using `Box` with aligned positioning:
```kotlin
Box(modifier) {
    Canvas(Modifier.fillMaxSize()) { /* draw chart */ }
    // Axis labels as Compose Text
    data.forEachIndexed { i, item ->
        Text(
            item.label,
            modifier = Modifier.align(...)
                .offset(x = computeX(i).dp, y = computeY(i).dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
```

## Activity Heatmap (GitHub-style)

```kotlin
@Composable
fun StudyHeatmap(
    data: List<StudyHeatmapDay>,
    modifier: Modifier = Modifier,
) {
    // 7 rows (Mon-Sun) × N columns (weeks)
    // Color intensity: 0=empty, 1-5=light, 6-15=medium, 16+=dark
    // Use LazyRow of Column(7 cells) for scrollable heatmap
    // Each cell: Box with background color from gradient
}
```

Color scale: `surfaceVariant` → `primary.copy(alpha=0.3)` → `primary.copy(alpha=0.6)` → `primary`

## Level Distribution Bar

```kotlin
@Composable
fun LevelDistributionBar(
    levels: List<AccuracyByLevel>,
    modifier: Modifier = Modifier,
) {
    // Horizontal stacked bar or vertical grouped bars
    // Each level gets a segment proportional to totalReviews
    // Color: gradient from error (low accuracy) to primary (high accuracy)
    // Label: "L0: 72%" ... "L6: 95%"
}
```

## Design Rules

1. **Theme colors only** — use `MaterialTheme.colorScheme.*`, never hardcode hex
2. **Responsive** — use `fillMaxWidth()` and compute positions from `size.width/height`
3. **Animations** — use `animateFloatAsState` for bar heights, `Animatable` for drawing paths
4. **Empty state** — if data is empty, show encouraging message, not blank chart
5. **Accessibility** — add `contentDescription` to the chart container with summary text
6. **Performance** — use `remember` for computed paths/positions, avoid recomputation on recompose
7. **Touch** — consider adding `pointerInput` for tap-to-inspect data points

## Existing Components to Reuse

Check `design-system/` and `feature/study/ui/study/` before creating new components:
- `WordDistributionBar` — existing level distribution bar (study screen)
- `ProgressRing` — circular progress indicator
- `ProportionalBar` — proportional segment bar
- `WeeklyActivitySection` — weekly activity display

Read these first and extend or adapt before building from scratch.
