---
name: share-card-generator
description: Generate shareable image cards from analytics data — milestone achievements, daily recaps, year-in-review, difficulty conquered. Renders Compose Canvas to bitmap with branded design for social sharing
tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash"]
model: sonnet
skills: ["analytics-feature", "design-system", "screen-patterns"]
---

# Share Card Generator

Build shareable image cards that users can export to social media showing their learning achievements.

## Card Architecture

```
ShareCardViewModel (loads data) → ShareCardScreen (preview + share button) → ShareCardRenderer (Canvas → Bitmap → Share)
```

### Card types to support:
- **Daily recap**: "Today I reviewed X words with Y% accuracy"
- **Milestone**: "Just mastered my 100th word!"
- **Streak**: "30 days of daily practice"
- **Difficulty conquered**: "Finally mastered 'word' after N attempts"
- **Year/Month in review**: Infographic summary

## Implementation Pattern

### 1. Card Data Model (domain)
```kotlin
sealed class ShareCardData {
    data class DailyRecap(val date: String, val cardsReviewed: Int, val accuracy: Double, val language: String) : ShareCardData()
    data class Milestone(val type: MilestoneType, val value: Long) : ShareCardData()
    data class StreakAchievement(val days: Int) : ShareCardData()
    data class WordConquered(val word: String, val translation: String, val attempts: Int) : ShareCardData()
}
```

### 2. Card Composable (stateless, receives data)
```kotlin
@Composable
fun ShareCard(data: ShareCardData, modifier: Modifier = Modifier) {
    // Fixed aspect ratio (1080x1920 or 1080x1080)
    // Brand colors + logo
    // Data-driven content
}
```

### 3. Bitmap Export (platform expect/actual)
```kotlin
// Common
expect class ShareCardExporter {
    suspend fun exportAndShare(cardData: ShareCardData)
}
// Android: use ComposeView + drawToBitmap + ShareSheet
// iOS: use UIGraphicsImageRenderer + UIActivityViewController
```

## Design Guidelines

- Card dimensions: 1080x1350px (Instagram post) or 1080x1920px (story)
- Background: gradient using brand primary colors
- Text: white on dark gradient, bold headline + light body
- Always include app logo/watermark at bottom
- Use `MaterialTheme.colorScheme.primary` / `tertiary` for accents
- Accessibility: ensure contrast ratio >= 4.5:1

## Data Sources

All data comes from existing analytics use cases:
- `GetStudyInsightsUseCase` → total cards, accuracy, mastered count
- `GetDifficultWordsUseCase` → word conquered cards
- `IAnalyticsRepository.getStudyInsights()` → overview stats
- Streak data from existing `IStreakRepository`
