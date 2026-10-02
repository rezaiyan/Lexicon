# Learning Focus — Design

**Date:** 2026-10-02
**Status:** Approved design, not implemented
**Scope:** Client only (Lexicon KMP). No backend changes.

## Problem

Users can hold words from several learning languages. Study mixes them all into one queue, which hurts focus. Goal: one active ("focus") language at a time, chosen smartly, switchable in one tap, invisible to single-language users, never blocking any action.

## Decisions

| Topic | Decision |
|---|---|
| Inactive-language words | Hidden from study, SRS keeps aging (no pause, no schedule rewrite) |
| Storage | Local settings only; smart default re-derives on new devices |
| Opt-out | "All languages" option = today's mixed behavior |
| Stats scope | Progress / level buckets / tags scoped to focus; streak + daily activity global |
| Learning language | `Word.sourceLanguage` (the word being learned); `targetLanguage` = translation side |
| Approach | Domain-level focus stream; in-memory filtering in use cases; no SQL changes |

## Domain

```kotlin
sealed interface LearningFocus {
    data class Single(val language: Language) : LearningFocus
    data object All : LearningFocus
}

data class LanguageSummary(
    val language: Language,
    val wordCount: Int,
    val dueCount: Int,
)

interface ILearningFocusRepository {
    fun observePreference(): Flow<LearningFocus?>          // null = never chosen
    suspend fun setPreference(focus: LearningFocus): Try<Unit>
    fun observeNudgeDismissals(): Flow<Map<Language, LocalDate>>
    suspend fun dismissNudge(language: Language, date: LocalDate): Try<Unit>
    fun observeIntroAcknowledged(): Flow<Boolean>
    suspend fun acknowledgeIntro(): Try<Unit>
}
```

Use cases (`domain/.../focus/usecase/`):

- `ObserveLearningFocusUseCase : NoParamFlowUseCase<LearningFocus>` resolves:
  1. Distinct `sourceLanguage` count ≤ 1 → `All` (identical behavior; guarantees `Single` implies 2+ languages).
  2. Stored `All` → `All`.
  3. Stored `Single(x)` and `x` still present in words → `Single(x)`.
  4. Otherwise smart default: language of most recently reviewed word (`lastReviewDate` max); tie / no reviews → language with most due cards; tie → most words.
- `GetLanguageOverviewUseCase : NoParamFlowUseCase<List<LanguageSummary>>`, sorted active first, then `dueCount` desc.
- `SetLearningFocusUseCase : UseCase<LearningFocus, Unit>`
- Nudge: the non-active language with the most due cards when `dueCount >= 10`, focus is not `All`, and the nudge was not dismissed today (one global dismissal per day, stored as UTC epoch day).
- `ObserveStudyFocusUseCase` combines words, tags and preferences into one `StudyFocusOverview` (focus, languages, nudge, showIntro, scoped progress stats, scoped tag stats) for the Study screen.
- `DismissFocusNudgeUseCase`, `AcknowledgeFocusIntroUseCase`.
- Shared helper: `List<Word>.filterBy(focus)` extension in domain.

### Scoped by focus

- `LoadReviewQueueUseCase`: all `ReviewSource` variants; daily-goal cap applied **after** filtering
- Study progress stats, level buckets, `TagsSection` counts (tags with 0 focused words hidden)
- Word Rush word selection
- `GetDailyWidgetDataUseCase` due count
- `ScheduleNotificationsUseCase` due count; reschedule on focus change
- Default `sourceLanguage` for add-word, file import, AI import

### Global (unchanged)

- Streak, daily activity, backend analytics
- Backend review-reminder push (server unaware of focus; text has no language name)
- Word Manager data (only its filter's initial value follows focus)

### Performance

Resolver + overview + nudge built from one `combine` over the existing words `Flow` + preference flows, `flowOn(Dispatchers.Default)`, `distinctUntilChanged()`.

## Data

- `LearningFocusRepositoryImpl` in `data`, backed by the same local settings store as `SettingsRepositoryImpl`.
- Keys: `learning_focus` (`"all"` or language code), `focus_nudge_dismissed_<code>` (ISO date), `focus_intro_ack` (bool).
- Cleared by `clearSettings()` (logout, delete account, clear data).
- DI: repository `single`, use cases `factory`, registered via `AppModule.kt` modules.

## UI

- **`LanguageBadge`** (design-system): round chip with 2-letter code (`DE`), tint passed in by the caller; presentation maps language → stable `Theme.*` accent. No flag emoji.
- **`LearningFocusChip`** (Study header, above stats): `[DE] Deutsch ▾` or `[⋯] All languages ▾`. Hidden when fewer than 2 languages. Tap opens the switcher.
- **`LanguageSwitcherSheetContent`** (`showSizeToFitBottomSheet`): a row per language (badge, native name, `N words · M due`, ✓ if active, ● if overdue), then the "All languages (mixed review)" row. Tap switches and closes; no confirm.
- After a switch: stats, due count and Review button crossfade / count to the new values.
- **`FocusNudgeCard`** (below the Review button): `[ES] 14 Spanish words are waiting · Switch` with `✕` dismiss (hidden for that language until the next day).
- **`FocusIntroCard`**: inline, shown once when the 2nd language first appears: "You're learning 2 languages. Focus on one at a time — switch anytime here." `Got it`. Non-blocking.
- **Add / import**: language preselected to focus. Adding another language saves it normally and shows the snackbar "Added to Español". Focus never changes implicitly.
- **Word Manager**: language filter starts at focus; "All" one tap away; changing it does not change study focus.
- **Review / Word Rush**: small badge in the session header; focus can't be changed mid-session.

`StudyProgressViewModel` state adds `focus: LearningFocus`, `languages: ImmutableList<LanguageSummary>`, `nudge: LanguageSummary?`, `showIntro: Boolean`. Event sink: `selectFocus(focus)`, `dismissNudge(language)`, `acknowledgeIntro()`.

## Edge cases

| Case | Behavior |
|---|---|
| 0 words | `All`, chip hidden |
| 1 language | Chip hidden; behavior identical to today |
| 2nd language appears (add/import/sync) | Smart default applied; intro shown once |
| Active language's last word deleted / batch-moved | Resolver falls back to smart default; stored pref kept and reactivates if the language returns |
| Focus change during a session | Not possible from UI; queue loaded once per session |
| Active has 0 due, other has due | Normal "caught up" state + nudge if ≥10; no auto-switch |
| `All` | Everything unfiltered; no nudge |
| Logout / delete / clear | Preference, nudge dismissals, intro flag wiped |
| New device / fresh sync | No pref, so smart default (most recent review) |
| `ByTag` / `ByStage` | Filtered by focus |
| `sourceLanguage == targetLanguage` | Treated as a normal language |

## Testing (TDD, fakes, `commonTest`)

- `ObserveLearningFocusUseCaseTest`: 0/1/2+ languages, valid pref, stale pref fallback, smart-default ordering + ties, `All`, re-emit on word changes
- `GetLanguageOverviewUseCaseTest`: counts, sort, overdue
- `LearningFocusPolicyTest` (nudge): threshold, daily dismissal, never in `All`
- `SetLearningFocusUseCaseTest`
- `LoadReviewQueueUseCaseTest`: every `ReviewSource` × `Single`/`All`; cap after filter
- `GetDailyWidgetDataUseCaseTest`, `ScheduleNotificationsUseCaseTest`: scoped counts
- `LearningFocusRepositoryImplTest`: persist / read / cleared by `clearSettings()`
- `StudyProgressViewModelTest` (Turbine): focus state, `selectFocus`, `dismissNudge`, `acknowledgeIntro`, chip hidden with 1 language
- Word Manager VM: filter initial value = focus, independent afterwards
- `FakeLearningFocusRepository` in `test/src/commonMain/kotlin/fakes/`

**Manual verification:** Android build with two-language seed data. Switch focus and check queue/stats/widget; nudge appears; delete last word of the active language and check fallback. Then a `ux-reviewer` pass.

## Out of scope

Backend sync of focus, per-language daily goals or streaks, SRS pausing, flag icons, mid-session switching.

**Deferred to a follow-up (v1 ships without):** "Added to Español" import snackbar, focus badge in the Review / Word Rush session header, translations of the new strings (they fall back to English).

**Storage note:** focus lives in its own `LearningFocusEntity` table, not in `SettingsEntity` columns. `insertSettings` is `INSERT OR REPLACE` with an explicit column list, so any unlisted column resets on every settings save (this already affects `word_sync_timestamp`, tracked separately).
