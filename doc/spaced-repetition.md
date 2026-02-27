# Spaced Repetition System

## Overview
7-bucket system (levels 0-6) implemented in `ReviewWordUseCase`.
Based on SM-2 algorithm with configurable settings.

## Bucket Levels & Intervals

| Level | Name | Interval | Description |
|-------|------|----------|-------------|
| 0 | Fresh | 1 min | Brand new words |
| 1 | Learning | 10 min | First learning phase |
| 2 | Familiar | 1 day | Getting familiar |
| 3 | Building | 3 days | Building confidence |
| 4 | Almost | 7 days | Almost there |
| 5 | Strong | 14 days | Strong grasp |
| 6 | Mastered | 30+ days | Fully mastered (interval grows exponentially) |

## Review Quality

- **Quality 0 (FORGOT)**: Drop level by `forgotPenalty` (default 2), floor at 0
- **Quality 1 (REMEMBERED)**: Advance level after `successesToAdvance` consecutive successes (default 1)

## Current Settings (BALANCED preset)
```kotlin
ReviewSettings(successesToAdvance = 1, forgotPenalty = 2)
```
This means: advance on FIRST success, drop 2 levels on forget.

## Available Presets
```kotlin
EASY:      successesToAdvance = 1, forgotPenalty = 1  // Gentle
BALANCED:  successesToAdvance = 1, forgotPenalty = 2  // Default
RIGOROUS:  successesToAdvance = 2, forgotPenalty = 2  // Needs 2 successes
EXPERT:    successesToAdvance = 2, forgotPenalty = 3  // Hard mode
```

Note: Currently `GetReviewSettingsUseCase` always returns BALANCED (not user-configurable yet).

## Algorithm Details (`ReviewWordUseCase.invoke()`)

### On FORGOT (quality = 0):
```
newLevel = max(0, currentLevel - forgotPenalty)
repetitions = 0
interval = LEVEL_INTERVALS[newLevel]
easeFactor = max(1.3, easeFactor - 0.2)
nextReviewDate = now + intervalToMs(newInterval)
```

### On REMEMBERED (quality = 1):
```
repetitions += 1
if (repetitions >= successesToAdvance) {
    newLevel = min(6, currentLevel + 1)
    repetitions = 0
    if (newLevel == 6) {
        // Mastered: exponential growth
        interval = (interval * easeFactor).toInt()
        easeFactor = min(2.5, easeFactor + 0.1)
    } else {
        interval = LEVEL_INTERVALS[newLevel]
    }
} else {
    // Need more successes, stay at current level
    interval = LEVEL_INTERVALS[currentLevel]
}
nextReviewDate = now + intervalToMs(newInterval)
```

### Invalid Quality:
Treated as FORGOT with penalty applied.

## Interval Constants (LEVEL_INTERVALS)
```kotlin
0 → 1 minute
1 → 10 minutes
2 → 1 day (1440 minutes)
3 → 3 days (4320 minutes)
4 → 7 days (10080 minutes)
5 → 14 days (20160 minutes)
6 → 30 days (43200 minutes)
```

## Ease Factor
- Range: 1.3 to 2.5
- Decreases by 0.2 on FORGOT (floor 1.3)
- Increases by 0.1 on MASTERED review (cap 2.5)
- Used for exponential interval growth at Level 6

## Due Cards Query
```sql
SELECT * FROM WordEntity WHERE nextReviewDate <= :currentTime
```
Cards are "due" when `nextReviewDate` is in the past.

## Word Fields Updated on Review
- `level` (0-6)
- `easeFactor` (1.3-2.5)
- `interval` (minutes)
- `repetitions` (consecutive successes)
- `lastReviewDate` (epoch ms)
- `nextReviewDate` (epoch ms)

## Examples

### Word at Level 0, REMEMBERED:
- Level: 0 → 1
- Interval: 10 minutes
- Next review: now + 10 min

### Word at Level 3, FORGOT (penalty=2):
- Level: 3 → 1
- Interval: 10 minutes
- Ease factor: decreases by 0.2
- Next review: now + 10 min

### Word at Level 6 (Mastered), REMEMBERED:
- Stays at Level 6
- Interval: previous_interval * easeFactor (exponential growth)
- Ease factor: increases by 0.1 (cap 2.5)
