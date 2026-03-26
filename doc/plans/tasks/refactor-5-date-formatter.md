# REFACTOR-5 — Extract Epoch Date Formatter from SubscriptionViewModel

**Priority:** P3
**Status:** Open
**Wave:** 5 (low risk, consistency benefit)

## Problem

`SubscriptionViewModel` formats subscription expiration dates using a private `formatDate()`
method with hardcoded month abbreviations:

```kotlin
// SubscriptionViewModel.kt ~lines 71–80
private fun formatDate(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthName = monthNames[localDateTime.month.ordinal]
    return "$monthName ${localDateTime.day}, ${localDateTime.year}"
}
```

Problems:
- Untestable in isolation (buried in ViewModel)
- If another ViewModel needs the same format, it will duplicate this code
- The month list is off-by-one-prone (ordinal vs 1-based month index)

**Files with the problem:**
- `feature/subscription/src/commonMain/kotlin/feature/subscription/SubscriptionViewModel.kt`

## Files to Create

### Shared utility
```
domain/src/commonMain/kotlin/domain/common/util/EpochDateFormatter.kt
```
```kotlin
object EpochDateFormatter {
    fun toMediumDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String
}
```
- Format: `"Jan 15, 2025"`
- `timeZone` is injected (defaults to system) → testable with a fixed timezone
- Uses `kotlinx-datetime` only — no platform APIs

### Tests
```
domain/src/commonTest/kotlin/domain/common/util/EpochDateFormatterTest.kt
```

## Files to Modify

```
feature/subscription/.../SubscriptionViewModel.kt
```
- Delete `formatDate()` private method
- Replace call site with `EpochDateFormatter.toMediumDate(expirationDateMillis)`

No DI registration needed — `EpochDateFormatter` is a pure `object`, not a class with dependencies.

## Test Cases

### `EpochDateFormatterTest`
- Known epoch for 2025-01-15 → `"Jan 15, 2025"`
- Known epoch for 2024-12-01 → `"Dec 1, 2024"`
- Known epoch for 2024-02-29 → `"Feb 29, 2024"` (leap year)
- Known epoch for 2025-03-31 → `"Mar 31, 2025"` (month end)
- All 12 months covered (one test per month or a parameterised loop)
- `timeZone = TimeZone.UTC` produces deterministic output regardless of CI machine locale

## Acceptance Criteria

- [ ] `formatDate()` deleted from `SubscriptionViewModel`
- [ ] `EpochDateFormatter.toMediumDate()` tested for all 12 months
- [ ] Tests use `TimeZone.UTC` for determinism
- [ ] No `Calendar`, `DateFormat`, or platform-specific date APIs used
- [ ] `./gradlew composeApp:compileKotlinMetadata` passes
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
