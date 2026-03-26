# REFACTOR-3 — Extract Profile Stats Enrichment from ProfileViewModel

**Priority:** P2
**Status:** Open
**Wave:** 3 (medium risk — involves date logic)

## Problem

`ProfileViewModel` contains private extension functions that transform raw `ProfileStats` into
UI-ready models. Three domain concerns are embedded there:

1. **`isToday` computation** — compares an activity's date string against today's date.
   `Clock.System.now()` is called inside `toUiModel()`, making this untestable.
2. **Day-of-week label mapping** — hardcoded `when` over all 7 `DayOfWeek` values producing
   abbreviated strings ("MON", "TUE", …). Repeated domain knowledge that could diverge if
   copied.
3. **Sorting** — `weeklyActivity.sortedBy { it.date }` is data ordering logic (not UI order),
   currently inside the ViewModel's state builder.

**Files with the problem:**
- `feature/profile/src/commonMain/kotlin/feature/profile/ProfileViewModel.kt`
  - `ProfileStats.toUiModel()` ~lines 165–183
  - `DayActivity.toUiModel(todayStr)` ~lines 185–203

## Files to Create

### Domain use case
```
domain/src/commonMain/kotlin/domain/profile/usecase/EnrichProfileStatsUseCase.kt
```
```kotlin
class EnrichProfileStatsUseCase {
    operator fun invoke(stats: ProfileStats, today: LocalDate): EnrichedProfileStats
}
```
- Maps each `DayActivity` to `EnrichedDayActivity` (adds `dayOfWeekLabel`, `isToday`, `dayOfMonth`)
- Sorts the weekly activity list by `date` ascending
- `today` is injected as a parameter — no `Clock.System.now()` inside the use case

### Domain model
```
domain/src/commonMain/kotlin/domain/profile/model/EnrichedProfileStats.kt
domain/src/commonMain/kotlin/domain/profile/model/EnrichedDayActivity.kt
```
`EnrichedDayActivity`: date, dayOfMonth, dayOfWeekLabel, reviewCount, isToday.
`EnrichedProfileStats`: wraps original `ProfileStats` fields + `List<EnrichedDayActivity>`.

### Tests
```
domain/src/commonTest/kotlin/domain/profile/EnrichProfileStatsUseCaseTest.kt
feature/profile/src/commonTest/kotlin/feature/profile/ProfileViewModelTest.kt
```

## Files to Modify

```
feature/profile/.../ProfileViewModel.kt
```
- Inject `EnrichProfileStatsUseCase` via constructor
- Replace `ProfileStats.toUiModel()` body with:
  ```kotlin
  val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
  enrichProfileStatsUseCase(stats, today)
  ```
- Delete `ProfileStats.toUiModel()` private extension
- Delete `DayActivity.toUiModel()` private extension
- Update state builder to use `EnrichedProfileStats` fields directly

```
composeApp/src/commonMain/kotlin/di/AppModule.kt
```
Add:
```kotlin
factoryOf(::EnrichProfileStatsUseCase)
```

## Test Cases

### `EnrichProfileStatsUseCaseTest`

**Day-of-week labels:**
- Activity on a Monday → `dayOfWeekLabel == "MON"`
- Activity on a Sunday → `dayOfWeekLabel == "SUN"`
- All 7 weekdays produce correct 3-letter abbreviation

**`isToday` logic:**
- Activity date matches `today` → `isToday == true`
- Activity date is yesterday → `isToday == false`
- Activity date is tomorrow → `isToday == false`

**Sorting:**
- Input with out-of-order dates → output sorted ascending by date
- Already sorted input → unchanged order

**`dayOfMonth`:**
- Activity on 2025-03-05 → `dayOfMonth == 5`

**Empty activity list:**
- `stats` with empty `weeklyActivity` → `EnrichedProfileStats.weeklyActivity` is empty list

### `ProfileViewModelTest`
- On `refreshProfileStats()` success → state contains `EnrichedProfileStats` from use case (fake returns canned value)
- `EnrichProfileStatsUseCase` is called with today's date (not a hardcoded date)
- On stats load failure → state reflects error, enrichment use case not called

## Acceptance Criteria

- [ ] `ProfileStats.toUiModel()` deleted from `ProfileViewModel`
- [ ] `DayActivity.toUiModel()` deleted from `ProfileViewModel`
- [ ] No `Clock.System.now()` calls inside `EnrichProfileStatsUseCase`
- [ ] All 7 day-of-week labels tested
- [ ] `isToday` logic tested for today, yesterday, tomorrow
- [ ] Sort order tested with unsorted input
- [ ] `ProfileViewModelTest` uses a fake for `EnrichProfileStatsUseCase`
- [ ] `./gradlew composeApp:compileKotlinMetadata` passes
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
