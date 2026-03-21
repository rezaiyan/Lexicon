# Analytics & User Behaviour Tracking — Implementation Plan

> Scan date: 2026-03-21
> Covers: Lexicon KMP client + vokab.server backend
> Observability stack: Firebase Analytics/Crashlytics/Performance (client) · Metabase (backend BI) · PostgreSQL (source of truth)

---

## Baseline: What Already Works

Before the plan, here is what is already solid and should NOT be changed:

| Area | Status | Notes |
|---|---|---|
| Study session recording | ✅ | `study_sessions` table, synced via `/analytics/sync` |
| Per-card review events | ✅ | `review_events` with response time, level transitions |
| Streaks & daily activity | ✅ | `DailyActivity` + `StreakService`, current + longest streak |
| 22 analytics API endpoints | ✅ | Heatmap, difficult words, accuracy by hour/day, weekly report, etc. |
| Auth audit log | ✅ | Login, token refresh, logout, deletion with IP + user agent |
| Generic `app_events` table | ✅ | Flexible JSON properties, platform + version fields |
| Android Firebase Analytics | ✅ | `AndroidAnalyticsTracker` wired to Firebase |
| Android Firebase Crashlytics | ✅ | Crash reporting active |
| Android Firebase Performance | ✅ | `AndroidPerformanceTracer` with custom traces |
| Metabase deployed | ✅ | Running on port 3001, connected to PostgreSQL |
| `IAnalyticsTracker` interface | ✅ | Well-designed with 15+ methods |
| Auth events (client) | ✅ | Login success/fail, logout, token refresh tracked |
| Study session analytics (client) | ✅ | `ReviewViewModel` fires session start/end/card events |

---

## Phase 1 — Critical Fixes (Zero-Data Blockers)

> These ship first. Until they are done, analytics data is fundamentally incomplete.

### 1.1 Fix iOS Analytics (currently a complete no-op)

**Problem**: `IOSAnalyticsTracker.kt` only calls `NSLog`. Every iOS user is invisible.

**Root cause**: The Kotlin/Native `IOSAnalyticsTracker` just logs to console with a comment saying "actual Firebase tracking is handled by Swift layer" — but the Swift layer (`FirebaseAnalyticsHelper`) is never called from Kotlin.

**Solution**: Use the `expect/actual` pattern to call through to Swift.

**Files to change**:

```
platforms/src/iosMain/kotlin/analytics/IOSAnalyticsTracker.kt   ← implement real calls
iosApp/iosApp/FirebaseAnalyticsHelper.swift                      ← verify/create Swift bridge
```

**Implementation**:

```kotlin
// IOSAnalyticsTracker.kt — replace NSLog stubs with real calls
actual class IOSAnalyticsTracker : IAnalyticsTracker {
    override fun logScreenView(screenName: String) {
        FirebaseAnalyticsHelper.shared.logScreenView(screenName)
    }
    override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
        FirebaseAnalyticsHelper.shared.logEvent(eventName, parameters?.toNSDictionary())
    }
    override fun logError(throwable: Throwable, context: String) {
        FirebaseCrashlyticsHelper.shared.recordError(throwable, context)
    }
    // ... all other methods delegate to Swift helpers
}
```

```swift
// FirebaseAnalyticsHelper.swift
import FirebaseAnalytics

@objc class FirebaseAnalyticsHelper: NSObject {
    @objc static let shared = FirebaseAnalyticsHelper()

    @objc func logScreenView(_ screenName: String) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName
        ])
    }

    @objc func logEvent(_ name: String, parameters: NSDictionary?) {
        Analytics.logEvent(name, parameters: parameters as? [String: Any])
    }
}
```

**Acceptance criteria**: iOS events appear in Firebase DebugView within 1 session.

---

### 1.2 Add Screen Navigation Tracking

**Problem**: `NavigationGraph.kt` never calls `logScreenView()`. Tab switches and screen transitions are invisible — impossible to build any funnel without this.

**File to change**:
```
presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt
```

**Implementation**: Hook into the `NavController` and fire on destination change.

```kotlin
val navController = rememberNavController()
val analyticsTracker = koinInject<IAnalyticsTracker>()

// Track destination changes
LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
        val screenName = when (destination.route) {
            StudyRoute::class.qualifiedName -> "study"
            WordsRoute::class.qualifiedName -> "words"
            InsightsRoute::class.qualifiedName -> "insights"
            SettingsRoute::class.qualifiedName -> "settings"
            LeaderboardRoute::class.qualifiedName -> "leaderboard"
            ImportRoute::class.qualifiedName -> "import"
            SubscriptionRoute::class.qualifiedName -> "subscription"
            OnboardingRoute::class.qualifiedName -> "onboarding"
            else -> destination.route ?: "unknown"
        }
        analyticsTracker.logScreenView(screenName)
    }
}
```

**Screens to track** (minimum set):

| Screen Name | Route |
|---|---|
| `study` | Study/Review tab |
| `words` | Word manager |
| `insights` | Analytics/insights |
| `settings` | Settings |
| `leaderboard` | Leaderboard |
| `import` | AI import |
| `subscription` | Paywall |
| `onboarding_*` | Each onboarding step |

**Acceptance criteria**: Firebase DebugView shows `screen_view` events on every tab change.

---

## Phase 2 — Wire Up Already-Defined Events

> `IAnalyticsTracker` has these methods. They exist. They are never called. This is the cheapest analytics win in the codebase.

### 2.1 `logStreakUpdated(days, isNewRecord)`

**Where to call**: After `RecordStreakActivityUseCase` succeeds, inside `ReviewViewModel`.

```kotlin
// ReviewViewModel.kt — after session end use case
val streakResult = recordStreakActivityUseCase(Unit)
streakResult.onSuccess { streakData ->
    analyticsTracker.logStreakUpdated(
        days = streakData.currentStreak,
        isNewRecord = streakData.currentStreak >= streakData.longestStreak
    )
}
```

**Milestone tracking already built into `IAnalyticsTracker`** (7, 30, 100, 365 days) — no extra work needed.

---

### 2.2 `logWordMastered(level)`

**Where to call**: In `ReviewViewModel`, after each card review where `newLevel == 6`.

```kotlin
// ReviewViewModel.kt — inside recordReviewEvent success handler
if (reviewResult.newLevel == 6) {
    analyticsTracker.logWordMastered(level = 6)
}
```

---

### 2.3 `logDailyGoalCompleted(cardsTarget, cardsActual)`

**Where to call**: In `StudyProgressViewModel` or wherever daily goal progress is evaluated.

```kotlin
// When cards reviewed crosses the daily target threshold
if (previousCount < dailyGoal && currentCount >= dailyGoal) {
    analyticsTracker.logDailyGoalCompleted(
        cardsTarget = dailyGoal,
        cardsActual = currentCount
    )
}
```

---

### 2.4 `logWordsImported(count, method)`

**Where to call**: `AiWordImportViewModel` — after import is confirmed and saved.

```kotlin
// AiWordImportViewModel.kt — after saveWordsUseCase succeeds
analyticsTracker.logWordsImported(count = words.size, method = "ai")
```

---

### 2.5 Update User Properties After Each Session

**Where to call**: End of `ReviewViewModel.endSession()`, using `updateUserProgress()`.

```kotlin
analyticsTracker.updateUserProgress(
    totalWords = userStats.totalWords,
    matureWords = userStats.masteredWords,
    currentStreak = userStats.currentStreak
)
```

This enables Firebase audience segmentation by streak length, mastery count, vocabulary size.

---

## Phase 3 — Missing Funnel Tracking

> These are product-critical flows with zero visibility today. Each is a self-contained ViewModel change with corresponding `app_events` entries on the backend (no migration needed — the table already has flexible JSON properties).

### 3.1 Onboarding Funnel

**File**: `feature/onboarding/src/commonMain/kotlin/feature/onboarding/OnboardingViewModel.kt`

**Events to add**:

```kotlin
// Step 1: App opens onboarding for the first time
analyticsTracker.logEvent("onboarding_started", mapOf("source" to source))

// Step 2-N: Each step viewed
analyticsTracker.logEvent("onboarding_step_viewed", mapOf("step" to stepIndex, "step_name" to stepName))

// Language selection
analyticsTracker.logEvent("onboarding_language_selected", mapOf(
    "source_language" to sourceLang,
    "target_language" to targetLang
))

// Level selection
analyticsTracker.logEvent("onboarding_level_selected", mapOf("level" to level))

// Completion
analyticsTracker.logEvent("onboarding_completed", mapOf(
    "steps_completed" to totalSteps,
    "duration_ms" to durationMs
))

// Skip / abandon
analyticsTracker.logEvent("onboarding_skipped", mapOf("at_step" to currentStep))
```

**Backend**: All above map to `POST /api/v1/events`. No schema change needed.

**Metabase query** (Phase 5):
```sql
SELECT
  properties->>'at_step' AS skip_step,
  COUNT(*) AS skipped
FROM app_events
WHERE event_name = 'onboarding_skipped'
GROUP BY 1
ORDER BY 1;
```

---

### 3.2 AI Import Funnel

**File**: `feature/import/src/commonMain/kotlin/feature/aiimport/AiWordImportViewModel.kt`

**Events to add**:

```kotlin
// User opens import screen
analyticsTracker.logEvent("import_started", mapOf("method" to "ai"))

// User types a topic and requests generation
analyticsTracker.logEvent("import_topic_entered", mapOf("topic_length" to topic.length))

// AI returns preview
analyticsTracker.logEvent("import_preview_shown", mapOf(
    "word_count" to words.size,
    "topic" to topic
))

// User accepts and saves
analyticsTracker.logWordsImported(count = words.size, method = "ai")
analyticsTracker.logEvent("import_confirmed", mapOf(
    "word_count" to words.size,
    "topic" to topic
))

// User cancels at any step
analyticsTracker.logEvent("import_cancelled", mapOf("at_step" to currentStep))

// AI call fails
analyticsTracker.logEvent("import_failed", mapOf(
    "error_type" to errorType,
    "topic" to topic
))
```

---

### 3.3 Subscription / Paywall Funnel

**File**: `feature/subscription/src/commonMain/kotlin/feature/subscription/SubscriptionViewModel.kt`

**Events to add**:

```kotlin
// Paywall shown (with entry point context)
analyticsTracker.logEvent("subscription_screen_viewed", mapOf(
    "entry_point" to entryPoint  // e.g. "import_limit", "settings", "deep_link"
))

// User taps a plan
analyticsTracker.logEvent("subscription_plan_selected", mapOf(
    "plan" to planId,  // e.g. "monthly", "annual"
    "price" to price
))

// Purchase flow started
analyticsTracker.logEvent("subscription_purchase_started", mapOf("plan" to planId))

// Purchase success (RevenueCat callback)
analyticsTracker.logEvent("subscription_purchase_success", mapOf(
    "plan" to planId,
    "price" to price,
    "currency" to currency
))

// Purchase failed
analyticsTracker.logEvent("subscription_purchase_failed", mapOf(
    "plan" to planId,
    "error_code" to errorCode
))

// Restore tapped
analyticsTracker.logEvent("subscription_restore_tapped", null)

// Restore result
analyticsTracker.logEvent("subscription_restore_result", mapOf(
    "success" to success,
    "restored_plan" to restoredPlan
))
```

**Set user property on subscribe**:
```kotlin
analyticsTracker.setUserProperty("subscription_status", "premium")
analyticsTracker.setUserProperty("subscription_plan", planId)
```

---

### 3.4 Leaderboard Engagement

**File**: `feature/leaderboard/src/commonMain/kotlin/feature/leaderboard/LeaderboardViewModel.kt`

**Events to add**:

```kotlin
// Leaderboard loaded
analyticsTracker.logEvent("leaderboard_viewed", mapOf(
    "user_rank" to userRank,
    "total_users" to totalUsers
))

// Rank changed since last view
if (rankChanged) {
    analyticsTracker.logEvent("leaderboard_rank_changed", mapOf(
        "from" to previousRank,
        "to" to currentRank,
        "direction" to if (currentRank < previousRank) "up" else "down"
    ))
}
```

---

## Phase 4 — Backend: Engagement & Retention Gaps

> Backend changes. Some require new columns or endpoints; most just need new `app_events` entries on the client side.

### 4.1 Daily App Open Tracking (DAU/MAU)

**Problem**: No way to measure Daily Active Users or Monthly Active Users today.

**Solution**: Client fires `app_opened` event on every foreground resume.

**Client change** — add to `AppLifecycleObserver` or platform app entry:
```kotlin
analyticsTracker.logEvent("app_opened", mapOf(
    "platform" to platform,
    "app_version" to appVersion,
    "time_since_last_open_ms" to timeSinceLastOpenMs
))
// Also POST to /api/v1/events so backend can compute DAU
```

**Backend**: No schema change — `app_events` already handles this. Add Metabase DAU query (Phase 5).

---

### 4.2 Push Notification Click-Through Tracking

**Problem**: `daily_insights.sent_via_push = true` exists but there is no way to know if the user opened the notification.

**Backend migration**:
```sql
-- V15__add_push_engagement_tracking.sql
ALTER TABLE daily_insights ADD COLUMN push_opened BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE daily_insights ADD COLUMN push_opened_at TIMESTAMP;

-- Also track general notification engagement in app_events:
-- event_name = 'notification_opened', properties = { type, insight_id }
```

**New endpoint**:
```kotlin
// POST /api/v1/insights/{id}/opened
@PostMapping("/{id}/opened")
fun markInsightOpened(@PathVariable id: Long, @AuthenticatedUser userId: Long)
```

**Client change**: On notification tap, fire:
```kotlin
analyticsTracker.logEvent("notification_opened", mapOf(
    "type" to notificationType,  // daily_insight | streak_reminder | goal_reminder
    "insight_id" to insightId
))
// Also call POST /api/v1/insights/{id}/opened
```

---

### 4.3 Forgetting Curve / SRS Effectiveness Analysis

**Problem**: `words` table stores `ease_factor`, `review_interval`, `repetitions`, `next_review_date` — but these are never analysed. There is no way to know if the SRS algorithm is actually working.

**New backend endpoint** — add to `AnalyticsController`:
```kotlin
// GET /api/v1/analytics/srs-effectiveness
// Returns: average interval growth, ease factor distribution, over-due cards %
data class SrsEffectivenessDto(
    val avgIntervalDays: Double,
    val avgEaseFactor: Double,
    val overdueCardsCount: Int,
    val overdueCardsPercent: Double,
    val levelIntervalDistribution: List<LevelIntervalData>  // avg interval per level
)
```

**SQL** (no migration needed):
```sql
SELECT
  level,
  AVG(review_interval) AS avg_interval_days,
  AVG(ease_factor) AS avg_ease_factor,
  COUNT(*) FILTER (WHERE next_review_date < NOW()) AS overdue_count
FROM words
WHERE user_id = :userId
GROUP BY level
ORDER BY level;
```

---

### 4.4 Retention Cohort Endpoint

**Problem**: Data exists to compute D7/D30 retention but there is no pre-built query or endpoint.

**New backend endpoint**:
```kotlin
// GET /api/v1/analytics/retention  (admin/internal use, drives Metabase)
data class RetentionCohortDto(
    val cohortWeek: LocalDate,
    val cohortSize: Int,
    val retained7d: Int,
    val retained30d: Int,
    val retained7dPercent: Double,
    val retained30dPercent: Double
)
```

**SQL**:
```sql
SELECT
  DATE_TRUNC('week', u.created_at)::date AS cohort_week,
  COUNT(DISTINCT u.id) AS cohort_size,
  COUNT(DISTINCT u.id) FILTER (
    WHERE EXISTS (
      SELECT 1 FROM daily_activities da
      WHERE da.user_id = u.id
        AND da.activity_date >= u.created_at::date + 7
    )
  ) AS retained_7d,
  COUNT(DISTINCT u.id) FILTER (
    WHERE EXISTS (
      SELECT 1 FROM daily_activities da
      WHERE da.user_id = u.id
        AND da.activity_date >= u.created_at::date + 30
    )
  ) AS retained_30d
FROM users u
GROUP BY 1
ORDER BY 1 DESC;
```

---

### 4.5 Subscription Funnel Endpoint

**Problem**: `subscriptions` table stores status and expiry but there is no funnel view.

**No migration needed**. Add Metabase query using `app_events` (Phase 3.3 events) + `subscriptions` table join.

---

## Phase 5 — Metabase Dashboards

> No code changes. Pure SQL saved questions organised into 5 dashboards. All data already exists after Phases 1–4.

### Dashboard 1: Product Health

| Card | Query | Key Metric |
|---|---|---|
| DAU 30-day | `app_events WHERE event_name='app_opened' GROUP BY date` | Daily active users |
| WAU trend | Same, weekly | Weekly active users |
| D7 / D30 Retention | Retention cohort query (§4.4) | % users returning |
| Avg session length | `study_sessions AVG(duration_ms)` | Engagement depth |
| Platform split | `app_events GROUP BY platform` | iOS vs Android |

---

### Dashboard 2: Learning Funnel

| Card | Query | Key Metric |
|---|---|---|
| Onboarding completion | `app_events WHERE event_name='onboarding_completed' / 'onboarding_started'` | Completion % |
| Onboarding drop-off by step | `app_events WHERE event_name='onboarding_skipped' GROUP BY step` | Step with most abandonment |
| First study session rate | Users who did a study session within 24h of signup | Activation rate |
| Import funnel | `import_started → import_preview_shown → import_confirmed` | Conversion % |
| Time to first study | `MIN(study_sessions.started_at) - users.created_at` | Activation speed |

---

### Dashboard 3: Study Quality

| Card | Query | Key Metric |
|---|---|---|
| Avg accuracy (global) | `review_events AVG(rating > 0)` | Overall accuracy |
| Accuracy trend (90d) | `daily_stats` endpoint data | Trend direction |
| Session abandonment rate | `study_sessions WHERE completed_normally = false` | % abandoned |
| Difficult words (top 20) | Existing `/analytics/difficult-words` endpoint | Problem words |
| Response time trend | Existing `/analytics/response-time-trend` | Learning speed |
| SRS effectiveness | Level vs avg interval (§4.3) | Algorithm health |

---

### Dashboard 4: Monetisation

| Card | Query | Key Metric |
|---|---|---|
| Subscription conversion | `subscription_screen_viewed → subscription_purchase_success` | Conversion % |
| Funnel drop-off | Each step in subscription funnel | Biggest drop-off |
| Plan distribution | `subscriptions GROUP BY plan_type` | Monthly vs annual |
| Churn by cohort | `subscriptions WHERE status='cancelled'` grouped by signup cohort | Churn rate |
| Paywall entry points | `subscription_screen_viewed GROUP BY entry_point` | Top triggers |

---

### Dashboard 5: User Health

| Card | Query | Key Metric |
|---|---|---|
| Streak distribution | `users GROUP BY current_streak` buckets | Engagement levels |
| Words mastered / user | `words WHERE level=6 GROUP BY user_id` distribution | Mastery rate |
| Daily goal completion rate | `logDailyGoalCompleted` events / `app_opened` events | Goal adherence |
| Comeback words | Existing `/analytics/comeback-words` | Regression rate |
| Churn risk score | Users with current_streak=0 AND last study > 7 days | At-risk users |

---

## Phase 6 — Firebase Backend Observability (Optional)

> Low priority. Only implement if server-side funnel stitching with client events is needed.

**Scenario**: A user subscribes on iOS. The RevenueCat webhook fires on the backend. You want to tie the server-side subscription event to the client-side Firebase Analytics user.

**Implementation**:
1. Client sends `firebase_app_instance_id` on login (store in `users` table)
2. Backend uses Firebase Admin SDK to log server-side events:
   ```kotlin
   // On subscription webhook received
   firebaseAnalytics.logEvent(userId, "server_subscription_activated", mapOf("plan" to plan))
   ```
3. Firebase stitches client + server events into one user journey

**Backend migration** (if pursuing):
```sql
-- V16__add_firebase_instance_id.sql
ALTER TABLE users ADD COLUMN firebase_instance_id TEXT;
```

**Verdict**: Implement only after Phases 1–5 are complete and you have a specific cross-platform funnel question that cannot be answered from Metabase alone.

---

## Implementation Order & Dependencies

```
Phase 1.1 (iOS fix)          ─── no deps ──────────────────── ship alone
Phase 1.2 (nav tracking)     ─── no deps ──────────────────── ship alone
Phase 2   (wire events)      ─── no deps ──────────────────── ship with Phase 1
Phase 3.1 (onboarding)       ─── requires Phase 1.2 ────────── ship after Phase 1
Phase 3.2 (import)           ─── requires Phase 1.2 ────────── ship after Phase 1
Phase 3.3 (subscription)     ─── requires Phase 1.2 ────────── ship after Phase 1
Phase 3.4 (leaderboard)      ─── requires Phase 1.2 ────────── ship after Phase 1
Phase 4.1 (app opens)        ─── no deps ──────────────────── can ship anytime
Phase 4.2 (push CTR)         ─── requires Phase 4.1 ─────────  backend PR
Phase 4.3 (SRS endpoint)     ─── no deps ──────────────────── backend PR
Phase 4.4 (retention)        ─── no deps ──────────────────── backend PR
Phase 5   (Metabase)         ─── requires Phase 3+4 ──────────  after data flows
Phase 6   (Firebase backend) ─── requires all phases ─────────  last
```

## Suggested PR Grouping

| PR | Scope | Risk |
|---|---|---|
| `analytics/ios-fix` | Phase 1.1 — iOS analytics bridge | Medium (iOS build) |
| `analytics/client-core` | Phase 1.2 + Phase 2 — nav tracking + wire 5 events | Low |
| `analytics/funnels` | Phase 3.1–3.4 — 4 ViewModel additions | Low |
| `analytics/backend-engagement` | Phase 4.1–4.4 — new endpoints + 1 migration | Low |
| `analytics/metabase-dashboards` | Phase 5 — Metabase saved questions | Zero (no code) |
| `analytics/firebase-backend` | Phase 6 — optional | Medium |

---

## Event Name Registry

Complete list of all new events added by this plan (for Firebase + `app_events`):

```
# Navigation
screen_view { screen_name }

# Onboarding
onboarding_started { source }
onboarding_step_viewed { step, step_name }
onboarding_language_selected { source_language, target_language }
onboarding_level_selected { level }
onboarding_completed { steps_completed, duration_ms }
onboarding_skipped { at_step }

# Import
import_started { method }
import_topic_entered { topic_length }
import_preview_shown { word_count, topic }
import_confirmed { word_count, topic }
import_cancelled { at_step }
import_failed { error_type, topic }

# Subscription
subscription_screen_viewed { entry_point }
subscription_plan_selected { plan, price }
subscription_purchase_started { plan }
subscription_purchase_success { plan, price, currency }
subscription_purchase_failed { plan, error_code }
subscription_restore_tapped
subscription_restore_result { success, restored_plan }

# Engagement
app_opened { platform, app_version, time_since_last_open_ms }
notification_opened { type, insight_id }
leaderboard_viewed { user_rank, total_users }
leaderboard_rank_changed { from, to, direction }

# Existing (to be wired up)
streak_updated { days, is_new_record }          ← logStreakUpdated
word_mastered { level }                          ← logWordMastered
daily_goal_completed { cards_target, cards_actual } ← logDailyGoalCompleted
words_imported { count, method }                 ← logWordsImported
```

---

## Files Changed Summary

### Client (Lexicon)

| File | Change |
|---|---|
| `platforms/src/iosMain/kotlin/analytics/IOSAnalyticsTracker.kt` | Implement real Firebase calls via Swift bridge |
| `iosApp/iosApp/FirebaseAnalyticsHelper.swift` | Create/verify Swift Firebase bridge |
| `presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt` | Add `OnDestinationChangedListener` |
| `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt` | Wire `logStreakUpdated`, `logWordMastered`, `updateUserProgress` |
| `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt` | Wire `logDailyGoalCompleted` |
| `feature/import/src/commonMain/kotlin/feature/aiimport/AiWordImportViewModel.kt` | Add full import funnel events |
| `feature/onboarding/src/commonMain/kotlin/feature/onboarding/OnboardingViewModel.kt` | Add full onboarding funnel events |
| `feature/subscription/src/commonMain/kotlin/feature/subscription/SubscriptionViewModel.kt` | Add full subscription funnel events |
| `feature/leaderboard/src/commonMain/kotlin/feature/leaderboard/LeaderboardViewModel.kt` | Add leaderboard engagement events |

### Backend (vokab.server)

| File | Change |
|---|---|
| `src/main/resources/db/migration/V15__add_push_engagement.sql` | Add `push_opened` columns to `daily_insights` |
| `src/main/kotlin/.../controller/InsightsController.kt` | Add `POST /{id}/opened` endpoint |
| `src/main/kotlin/.../controller/AnalyticsController.kt` | Add `/srs-effectiveness` and `/retention` endpoints |
| `src/main/kotlin/.../service/AnalyticsService.kt` | Implement SRS effectiveness + retention queries |
| `src/main/kotlin/.../dto/AnalyticsDto.kt` | Add `SrsEffectivenessDto`, `RetentionCohortDto` |

### Metabase

| Item | Type |
|---|---|
| Dashboard: Product Health | 5 saved questions |
| Dashboard: Learning Funnel | 5 saved questions |
| Dashboard: Study Quality | 6 saved questions |
| Dashboard: Monetisation | 5 saved questions |
| Dashboard: User Health | 5 saved questions |
