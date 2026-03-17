---
name: milestone-system
description: Build milestone detection, achievement celebrations, and streak systems using analytics data — confetti animations, achievement badges, shareable milestone cards, and streak shields
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
model: sonnet
skills: ["analytics-feature", "viewmodel-patterns", "screen-patterns", "design-system", "motion", "testing-patterns"]
---

# Milestone & Achievement System Builder

Build the milestone detection, celebration UI, and streak system.

## Milestone Categories

### Review Milestones (totalCardsReviewed)
- First Review, 100, 500, 1000, 5000, 10000 cards

### Mastery Milestones (wordsMasteredCount)
- First Mastered, 10, 25, 50, 100, 250, 500 words

### Streak Milestones (consecutive days studied)
- 3, 7, 14, 30, 60, 100, 365 days

### Accuracy Milestones
- First Perfect Session (100% on 10+ cards)
- Accuracy above 90% for 7 consecutive days

### Special Milestones
- Comeback Champion: mastered a word after 5+ failures (comeback_words endpoint)
- Polyglot: studied 3+ language pairs (language_stats endpoint)
- Night Owl / Early Bird: 50+ reviews before 7am or after 10pm (accuracy_by_hour)
- Speed Demon: average response time under 2 seconds for a session

## Architecture

```
EndStudySession → CheckMilestonesUseCase → List<Milestone> → CelebrationEffect → Full-screen animation
```

### Domain Model
```kotlin
data class Milestone(
    val id: String,               // "cards_100", "mastered_50", "streak_30"
    val type: MilestoneType,      // REVIEW, MASTERY, STREAK, ACCURACY, SPECIAL
    val title: String,            // "Century Club"
    val description: String,      // "Reviewed 100 cards"
    val icon: String,             // Emoji or icon name
    val achievedAt: Long,         // Epoch ms
    val isNew: Boolean,           // True if just achieved this session
)
```

### Detection Logic
```kotlin
class CheckMilestonesUseCase(
    private val repository: IAnalyticsRepository,
    private val milestoneStore: IMilestoneStore,  // Tracks which milestones already achieved
) : NoParamUseCase<List<Milestone>> {
    override suspend fun invoke(params: Unit): Try<List<Milestone>> = Try {
        val insights = repository.getStudyInsights().getOrNull() ?: return@Try emptyList()
        val achieved = milestoneStore.getAchievedIds()

        buildList {
            // Check each threshold
            REVIEW_THRESHOLDS.forEach { (threshold, id, title) ->
                if (insights.totalCardsReviewed >= threshold && id !in achieved) {
                    add(Milestone(id, REVIEW, title, ...))
                    milestoneStore.markAchieved(id)
                }
            }
            // ... similar for mastery, streak, special
        }
    }
}
```

### Milestone Store
Use a simple key-value approach — `SharedPreferences` / `NSUserDefaults` via existing platform bridges.
```kotlin
interface IMilestoneStore {
    suspend fun getAchievedIds(): Set<String>
    suspend fun markAchieved(id: String)
}
```

## Celebration UI

### Full-screen Celebration Overlay
```kotlin
// Triggered via ViewModel effect after session end
sealed class ReviewEffect {
    data class StartReview(val firstWord: Word) : ReviewEffect()
    data class CelebrateMilestone(val milestone: Milestone) : ReviewEffect()  // NEW
}
```

Show as `OverlayHost.showFullScreen`:
- Confetti animation (use Compottie or custom Canvas particles)
- Large icon/emoji
- Title + description
- "Share" button → share card
- Auto-dismiss after 5 seconds or tap

### Achievement Badge Grid
Dedicated screen showing all milestones (achieved = colored, locked = greyed out).
Use `LazyVerticalGrid` with `Card` items.

## Integration Points

1. **After session end** in `ReviewViewModel.onReviewSessionComplete()`:
   - Call `checkMilestonesUseCase()`
   - If new milestones, emit `CelebrateMilestone` effect
2. **Profile screen**: Add "Achievements" section showing badge grid
3. **InsightsScreen**: Show recent achievements in Overview tab
