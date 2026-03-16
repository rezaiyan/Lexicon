---
name: study-coach
description: Build AI-powered study coaching features — personalized tips, optimal study time suggestions, difficulty pattern detection, and smart notifications derived from analytics data patterns
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: opus
skills: ["analytics-feature", "viewmodel-patterns", "usecase-patterns", "testing-patterns"]
---

# Study Coach Builder

Build personalized coaching features that analyze study patterns and deliver actionable tips.

## Coaching Architecture

```
Analytics Data (backend) → Pattern Detection (use cases) → Tip Generation → UI Presentation
```

Coaching is **rule-based** (not ML) — derive tips from statistical patterns in the analytics data.

## Pattern Detection Rules

### 1. Optimal Study Time
```
IF accuracy_by_hour has entries with >= 10 reviews
THEN recommend hour with highest accuracy
TIP: "You're 23% more accurate studying at 9 PM"
```
**Data**: `GET /analytics/accuracy-by-hour`

### 2. Session Length Sweet Spot
```
IF sessions with < 15 cards have higher completion rate than > 30 cards
THEN recommend shorter sessions
TIP: "Your accuracy peaks in shorter sessions. Try 15 cards per session."
```
**Data**: `GET /analytics/sessions` + compare completion rates

### 3. SRS Level Bottleneck
```
IF accuracy at level N < 70% while level N-1 > 85%
THEN flag level N as bottleneck
TIP: "Level 3 words have 62% accuracy. Review them more before advancing."
```
**Data**: `GET /analytics/accuracy-by-level`

### 4. Speed-Accuracy Tradeoff
```
IF average_response_time < 1500ms AND accuracy < 75%
THEN suggest slowing down
TIP: "You're answering fast but missing words. Try pausing before responding."
```
**Data**: `GET /analytics/response-time-trend` + `GET /analytics/insights`

### 5. Language Imbalance
```
IF language pair A has 5x more reviews than language pair B
THEN suggest balancing
TIP: "You've reviewed 200 German words but only 30 Spanish."
```
**Data**: `GET /analytics/language-stats`

### 6. Plateau Detection
```
IF accuracy is flat (< 2% change) for 3+ weeks
THEN suggest strategy change
TIP: "Your accuracy has plateaued. Try adding context sentences."
```
**Data**: `GET /analytics/monthly-stats`

### 7. Comeback Celebration
```
IF comeback_words is non-empty
THEN celebrate persistence
TIP: "You mastered 'word' after struggling with it — that's resilience!"
```
**Data**: `GET /analytics/comeback-words`

## Implementation Pattern

### Use Case
```kotlin
class GetStudyTipsUseCase(
    private val repository: IAnalyticsRepository,
) : NoParamUseCase<List<StudyTip>> {
    override suspend fun invoke(params: Unit): Try<List<StudyTip>> = Try {
        val tips = mutableListOf<StudyTip>()

        // Each detector is a separate private method
        repository.getAccuracyByHourOfDay().getOrNull()?.let { detectOptimalTime(it, tips) }
        repository.getAccuracyByLevel().getOrNull()?.let { detectBottleneck(it, tips) }
        repository.getLanguagePairStats().getOrNull()?.let { detectImbalance(it, tips) }

        tips.sortedByDescending { it.priority }
    }
}
```

### Domain Model
```kotlin
data class StudyTip(
    val id: String,                    // For dedup and dismissal tracking
    val type: TipType,                 // OPTIMAL_TIME, BOTTLENECK, SPEED_ACCURACY, etc.
    val title: String,                 // Short headline
    val message: String,               // Actionable description
    val priority: Int,                 // 1-10, higher = more important
    val actionLabel: String? = null,   // CTA button text
    val actionRoute: String? = null,   // Deep link to relevant screen
)
```

### UI Placement
- **Study screen**: Show top 1 tip as a dismissible card above the review button
- **Insights screen**: Dedicated "Coaching" tab with all tips
- **Post-session**: Show relevant tip after session complete (e.g., "Great session! You study best at this time")

## Tone Guidelines

- **Always positive framing**: "You're improving" not "You're failing"
- **Specific numbers**: "23% more accurate" not "much better"
- **Actionable**: Every tip suggests a concrete next step
- **Non-intrusive**: Tips are suggestions, never blocking
- **Dismissible**: Users can dismiss tips they don't want
