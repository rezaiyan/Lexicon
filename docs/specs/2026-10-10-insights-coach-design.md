# Insights Coach — Design

**Date:** 2026-10-10
**Status:** Draft, pending review
**Scope:** Insights tab revamp (client + backend). Leaderboard revamp is a separate, later spec.

---

## 1. Problem

The Insights tab is a long scroll of ~10 descriptive cards (streak, weekly tiles, reviews/day,
collapsible overview, Word Rush, accuracy by level, day-of-week, level transitions, response time,
difficult words). Issues:

- **No "so what".** Stats describe the past; only two actions exist (study difficult words,
  reminder switch hidden inside a collapsed card).
- **Slow, fragile load.** Every tab visit fires 10 requests; the screen waits for the slowest one.
  Nothing works offline.
- **Wrong numbers for many users.** Weekly report and hour-based accuracy are computed in UTC, so
  "best time" and week boundaries are off for anyone not in UTC.
- **Hard to extend.** 818-line `InsightsScreen.kt`, mapping and date math inside the ViewModel,
  hardcoded English strings (`"sessions"`, `"% accuracy"`, month names).

## 2. Goals

1. In two seconds, the user knows **whether they are progressing** and **what to do next**.
2. Every number on screen is understandable without explanation (see §6).
3. New insight types ship with a **backend deploy only**, no app release.
4. Opening the tab is instant (cached) and works offline with last-known data.

### Non-goals

- Leaderboard changes (trophy icon entry stays as is).
- Server-side dismissal tracking, push delivery of coach cards.
- Non-English copy (Accept-Language plumbing is in place; translations later).

## 3. Approach

**Backend-generated screen payload.** One endpoint returns everything the tab needs: hero,
ranked coach cards, deep-dive sections, each with server-written captions. The client caches the
last response locally and renders it with a small set of generic + rich components.

Rejected:
- *Client-side rule engine over local+remote data* — cleaner offline story, but every new insight
  needs an app release.
- *UI restructure only* — fastest, but logic keeps piling up in the ViewModel.

## 4. Screen

Top to bottom:

1. **Hero — "This week"**
   - Headline sentence from server (e.g. "Your best week in a month").
   - Three big numbers: reviews, accuracy, words leveled up — each with a change vs last week.
   - 7 streak dots (Mon–Sun in user's locale week), today highlighted, current streak count.
2. **Coach cards** — up to 3, ranked by server priority. Each: icon, title, one-line reason,
   optional word chips (max 5), one action button. Dismiss hides the card for the rest of the
   local day.
3. **Deep dive** — collapsible sections, closed by default, each opens with a one-sentence takeaway
   caption *above* its chart:
   - **Mastery journey** — words per level (stacked bar), promotions/demotions this week.
   - **Habits** — 12-week heatmap, best hour, accuracy by weekday.
   - **Words** — hardest words (Review all) and comeback wins (once hard, now mastered).
   - **Word Rush** — games played, best score, last-5 score trend.
   A section the server omits (not enough data) shows an **"unlocks after N more reviews"** teaser
   with a progress bar instead of an empty chart.

States:
| State | UI |
|---|---|
| No cache, loading | Skeleton of hero + 2 cards |
| Cache present, refreshing | Cached content, no spinner |
| Cache present, refresh failed | Cached content + "Updated 2h ago" caption |
| No cache, failed | `ErrorScreen` with retry |
| New user (no reviews) | Welcome empty state with "Start your first review" button |

## 5. Coach rules (MVP)

Evaluated server-side in priority order; one card per type; top 3 returned.

| Type | Fires when | Action |
|---|---|---|
| `STREAK_AT_RISK` | studied yesterday, not today, local time ≥ 18:00 | `START_REVIEW` |
| `SLIPPING_WORDS` | ≥ 3 words demoted in last 7 local days | `REVIEW_WORDS(wordIds)` |
| `DIFFICULT_WORDS` | ≥ 3 words with < 50% accuracy over ≥ 3 reviews | `REVIEW_WORDS(wordIds)` |
| `MILESTONE_NEAR` | ≤ 10 words from next mastered milestone (50, 100, 250, 500, 1000, then every 500) | `START_REVIEW` |
| `BEST_TIME` | an hour with ≥ 30 reviews is ≥ 10 pt above overall accuracy, reminders off | `ENABLE_REMINDER(hour)` |
| `LEVEL_BOTTLENECK` | one level's pass rate ≥ 15 pt below the mean of others (each ≥ 20 reviews) | `REVIEW_WORDS(wordIds)` |
| `COMEBACK_WIN` | a word once in difficult set is now mastered | `NONE` |
| `WEEK_TREND` | always eligible (fallback, lowest priority) | `NONE` |

Thresholds live as named constants in each rule class, not magic numbers inline.

## 6. User-friendly data display rules

These apply to server copy and client rendering alike.

1. **Every number has context.** Show change vs last week with arrow + sign + word ("▲ 18 more
   than last week"), never a lone "-3%". Color is never the only signal (arrow/icon always present).
2. **Plain language.** "Words leveled up", not "level transitions". "Average answer time", not
   "response time trend". No internal terms (SRS, level index, ms).
3. **Sample-size gates.** No percentage from fewer than 20 reviews; no hour/weekday claim from fewer
   than 30 reviews in that bucket. Below the gate, the section shows the unlock teaser.
4. **Local time everywhere.** Client sends IANA zone; server computes weeks, days, hours in it.
   Hours render in device locale format ("8 PM" / "20:00"), weekdays start per locale.
5. **Friendly formatting.** Integer percentages; durations as "1h 20m" / "45m"; counts ≥ 1000 as
   "1.2k"; dates relative ("Today", "Yesterday", "Mon") within a week, "Oct 3" beyond.
6. **One takeaway per chart.** Every chart carries a server caption sentence ("You're sharpest on
   Tuesdays") and an accessibility `contentDescription` equal to that caption.
7. **Show words, not counts, when actionable.** Coach cards with word IDs show up to 5 word chips
   plus "+N more".
8. **Encourage, don't scold.** Drops are framed as next steps ("12 words need a refresh"), not
   failures.

## 7. API contract

`GET /api/v1/analytics/insights-screen?tz=Europe/Berlin`
Headers: `Authorization`, `Accept-Language`. `tz` falls back to UTC if missing or invalid.

```json
{
  "success": true,
  "data": {
    "generatedAt": "2026-10-10T18:04:00Z",
    "totalReviews": 1420,
    "hero": {
      "headline": "Your best week in a month",
      "reviews":     { "value": 212, "previous": 194 },
      "accuracyPct": { "value": 84,  "previous": 79 },
      "leveledUp":   { "value": 31,  "previous": 22 },
      "currentStreak": 6,
      "week": [ { "date": "2026-10-05", "reviews": 40 }, "...7 entries, locale week order..." ]
    },
    "coach": [
      {
        "id": "SLIPPING_WORDS:2026-10-10",
        "type": "SLIPPING_WORDS",
        "priority": 80,
        "title": "12 words need a refresh",
        "body": "They dropped a level this week. A quick review locks them back in.",
        "words": [ { "id": 51, "text": "ubiquitous" } ],
        "moreWordsCount": 7,
        "action": { "kind": "REVIEW_WORDS", "label": "Review 12 words", "wordIds": [51, 52] }
      }
    ],
    "sections": {
      "mastery":  { "caption": "...", "levels": [ { "level": 1, "words": 40 } ],
                    "promotedThisWeek": 31, "demotedThisWeek": 12 },
      "habits":   { "caption": "...", "heatmap": [ { "date": "2026-07-20", "reviews": 12 } ],
                    "bestHour": { "hour": 20, "accuracyPct": 91 },
                    "weekdays": [ { "isoDay": 2, "accuracyPct": 88, "reviews": 140 } ] },
      "words":    { "caption": "...", "hardest": [ { "id": 51, "text": "...", "accuracyPct": 33 } ],
                    "comebacks": [ { "id": 9, "text": "..." } ] },
      "wordRush": { "caption": "...", "gamesPlayed": 14, "bestScore": 2300, "recentScores": [1800, 2100] }
    },
    "locked": [ { "section": "habits", "reviewsNeeded": 18 } ]
  }
}
```

Contract rules:
- Any `sections.*` entry may be `null`; a null section with a `locked` entry renders the teaser,
  a null section without one is hidden.
- `action.kind` is a closed set: `REVIEW_WORDS`, `START_REVIEW`, `ENABLE_REMINDER` (+ `hour`),
  `START_WORD_RUSH`, `NONE`. Unknown kind → card renders without a button.
- Unknown `type` → client renders the generic card from `title`/`body`/`words`/`action`.
- Client DTOs use `ignoreUnknownKeys` and defaults for every optional field.
- `id` is stable per type per local day; client uses it for dismissal.

## 8. Backend design (`lexicon.server`)

Package `analytics/insightsscreen/`:

| Unit | Responsibility |
|---|---|
| `InsightsScreenController` | `GET /insights-screen`, parses `tz`, wraps in `ApiResponse` |
| `InsightsScreenService` | `@Transactional(readOnly = true)`; builds snapshot once, assembles hero, coach, sections |
| `LearnerSnapshot` | Immutable data loaded once per request: weekly + previous-week stats, per-level counts and pass rates, demotions, difficult/comeback words, hourly/weekday accuracy, heatmap, Word Rush stats, reminder setting, local `now` |
| `LearnerSnapshotLoader` | Builds `LearnerSnapshot` by reusing `StudyActivityQueries`, `AccuracyQueries`, `WordProgressQueries`, `WeeklyReportService` with a `ZoneId` |
| `CoachRule` (interface) | `val type: String; val priority: Int; fun evaluate(s: LearnerSnapshot): CoachCardDto?` |
| `rules/*Rule.kt` | One `@Component` per rule from §5 |
| `CoachEngine` | Injects `List<CoachRule>`; evaluates each in isolation (a throwing rule is logged and skipped), sorts by priority, takes 3 |
| `SectionBuilders` | One small function per deep-dive section; returns `null` + `locked` entry below sample gates |

Timezone fix: existing query methods gain a `ZoneId` parameter (default UTC to keep old endpoints
unchanged); `WeeklyReportService` and hour/weekday accuracy compute in that zone.

No DB migration required.

## 9. Client design (`Lexicon`)

**Domain** (`domain/insights/`)
- `InsightsScreen` model + sub-models (`WeeklyHero`, `Delta`, `CoachCard`, `CoachAction` sealed
  interface, `MasterySection`, `HabitsSection`, `WordsSection`, `WordRushSection`, `LockedSection`).
- `IInsightsScreenRepository`: `fun observe(): Flow<CachedInsights?>`, `suspend fun refresh(zone: String): Try<Unit>`.
- `ObserveInsightsScreenUseCase` (FlowUseCase), `RefreshInsightsScreenUseCase` (UseCase).

**Data** (`data/insights/`)
- `InsightsScreenRemoteDataSource` (Ktor) + DTOs + `toDomain()` mappers; unknown action kind →
  `CoachAction.None`.
- `InsightsCacheEntity` SQLDelight table (`json`, `fetchedAtMs`), new sequential `.sqm`;
  stale-while-revalidate: emit cache, refresh in background, replace on success. Cleared on logout.

**Presentation** (`feature/insights/`)
- `InsightsViewModel` — `BaseViewModel<InsightsState, InsightsEffect>`; state =
  `content: InsightsUiModel?`, `isRefreshing`, `lastUpdatedLabel`, `error`; event sink:
  `refresh()`, `onCoachAction(card)`, `dismissCard(id)`, `toggleSection(key)`.
- `InsightsUiMapper` — pure domain → UI mapping and all formatting (§6), unit tested.
  Dismissed IDs held in a small `DismissedCoachCardsStore` (date-scoped key-value).
- UI split into files, each < 300 lines: `InsightsScreen.kt` (scaffold + states),
  `HeroSection.kt`, `CoachCardList.kt`, `CoachCard.kt` (generic), `coach/RichCoachContent.kt`
  (type → optional rich slot, e.g. word chips, milestone progress ring), `DeepDiveSection.kt`
  (collapsible shell + locked teaser), and one file per section.
- Reuse design-system components (`AccentCard`, `Pill`, theme tokens); new generic pieces
  (`DeltaLabel`, `LockedTeaser`) go to `design-system` only if domain-free.
- Effects: `NavigateToReview(wordIds)`, `StartReview`, `StartWordRush`, `OpenNotificationSettings`.
  `ENABLE_REMINDER` calls existing `SetReviewRemindersEnabledUseCase`.
- Remove the 10 per-section use cases from `InsightsUseCases`; keep the use cases themselves only if
  other screens still reference them (verify in plan).

**Analytics events:** `insights_viewed`, `coach_card_shown(type)`, `coach_card_action(type)`,
`coach_card_dismissed(type)` — feeds future rule tuning.

## 10. Error handling

- Server: each rule and section builder isolated; failure logged, item skipped, endpoint still 200.
  Only snapshot-load failure returns 500.
- Client: refresh failure never clears cache. Malformed payload → `DomainError` via `Try`, cache
  kept. No `!!`, no try/catch control flow.

## 11. Testing

Backend:
- One unit test class per rule (fires / does not fire / threshold edges) with a `LearnerSnapshot`
  test builder.
- `CoachEngine` test: ordering, cap of 3, throwing rule skipped.
- Timezone tests: week boundary and best hour for `Asia/Tokyo` vs `America/Los_Angeles`.
- Controller test (`ControllerTestSecurityConfig`) + service integration test (Testcontainers).

Client:
- DTO parsing: unknown `type`, unknown `action.kind`, null sections, missing optional fields.
- `InsightsUiMapper`: deltas, sample-gate teasers, formatting (1.2k, 1h 20m, relative dates, hour
  locale).
- Repository with fake data sources: cache-then-refresh, failure keeps cache.
- ViewModel (Turbine): state transitions, coach action → effect, dismissal persists for the day.
- Maestro flow: open Insights, see hero + a coach card, tap action, land in review.

## 12. Rollout

1. Backend endpoint + timezone fix, deploy (`ali server`), verify with a real account.
2. Client behind existing feature-flag provider (`insights_coach`), old screen kept until flag is
   default-on; then delete old screen code.
3. Leaderboard spec starts after client ships.
