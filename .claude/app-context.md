# Lexicon App — Critical Functionality Reference

> Use this file to understand the full scope of the app before implementing any feature, fix, or refactor. Cross-reference with skills in `.claude/skills/` for implementation patterns.

---

## What the App Is

Lexicon is an **offline-first, multilingual vocabulary learning app** for Android and iOS (KMP). Users add words, study them via a 7-level spaced repetition system, and track their progress through rich analytics. The app supports Google/Apple Sign-In, cloud sync, leaderboards, TTS, AI-powered import, subscriptions (RevenueCat), and push notifications.

---

## App Startup Flow

```
App Launch
  └─ AppNavigationViewModel
       ├─ AuthPhase.Verifying → VerifySessionUseCase
       │    ├─ Valid session → check onboarding
       │    │    ├─ Has words → AppUiState.Ready (Study/Settings tabs)
       │    │    └─ No words  → Onboarding
       │    └─ No/expired session → AppUiState.LoginRequired
       └─ Auth events → Login → sync words → init push + subscriptions
```

**Key file**: `presentation/src/commonMain/kotlin/presentation/viewmodel/AppNavigationViewModel.kt`

---

## Feature Areas

### 1. Study / Spaced Repetition

**How it works (7-bucket SM2-like algorithm):**

| Level | Name | Review interval |
|-------|------|----------------|
| 0 | Fresh | 1 minute |
| 1 | Learning | 10 minutes |
| 2 | Familiar | 1 day |
| 3 | Building | 3 days |
| 4 | Almost | 7 days |
| 5 | Strong | 14 days |
| 6 | Mastered | 30+ days (grows exponentially) |

**Review result logic:**
- **Remembered** → increment repetitions; advance level when `repetitions >= successesToAdvance`; ease factor +0.1 (max 2.5)
- **Forgot** → drop by `forgotPenalty` levels; reset repetitions; ease factor -0.2 (min 1.3)

**Configurable presets**: EASY / BALANCED (default) / RIGOROUS / EXPERT
**Key use case**: `ReviewWordUseCase`
**ViewModels**: `ReviewViewModel`, `StudyProgressViewModel`

Study sessions are tracked: start time, end time, cards reviewed, accuracy, completion status.

---

### 2. Authentication

**Providers**: Google OAuth, Apple Sign-In
**Storage**: JWT tokens in platform secure storage (Android Keystore / iOS Keychain)
**Subscription statuses**: FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED

**Use cases**: `LoginWithGoogleUseCase`, `LoginWithAppleUseCase`, `VerifySessionUseCase`, `LogoutUseCase`, `DeleteAccountUseCase`, `GetFeatureAccessUseCase`

On login: sync words from backend → init push notifications → init RevenueCat.

---

### 3. Word Management

**Word model fields**: `id`, `originalWord`, `translation`, `description`, `sourceLanguage`, `targetLanguage`, `level` (0–6), `easeFactor`, `interval`, `repetitions`, `lastReviewDate`, `nextReviewDate`, `dateAdded`

**Key operations**:
- CRUD: `GetAllWordsUseCase`, `UpdateWordUseCase`, `DeleteWordUseCase`, `DeleteWordsUseCase`
- Filtering: `GetDueWordsUseCase`, `GetWordsByStageUseCase`
- Bulk: `BatchUpdateLanguagesUseCase`, `DeleteWordsUseCase` (with progress)
- Duplicate detection: `isSameContent()` — compares `originalWord` + `translation`
- Progress stats: `GetProgressStatsUseCase` → counts per level, due count

**Sync**: `SyncRemoteToLocalUseCase` (backend is source of truth on fresh sync)

---

### 4. Import

Three import paths:

| Path | Use case | Description |
|------|----------|-------------|
| **File** | `ImportViaFileUseCase` | CSV / plain text, word-translation pairs |
| **AI Image** | `ImportFromImageUseCase` | Extract vocabulary from a photo via AI |
| **AI Text** | (within ImportViewModel) | Paste text, AI extracts vocabulary |
| **Onboarding** | `ImportSuggestedVocabularyUseCase` | AI-suggested words based on user preferences |

Multi-step wizard for AI import: pick image → extraction type → target language → preview → import.

---

### 5. Analytics & Insights

**Events recorded per review**: sessionId, wordId, word/translation text, languages, rating (correct/incorrect), previousLevel, newLevel, responseTimeMs, timestamp.

**Session-level metrics**: totalCards, correctCount, incorrectCount, durationMs, reviewType, completedNormally.

**Insight queries available**:

| Query | Description |
|-------|-------------|
| `GetStudyInsightsUseCase` | Lifetime totals: cards, accuracy, time, sessions, words mastered |
| `GetAccuracyTrendUseCase` | 30-day daily accuracy trend |
| `GetDifficultWordsUseCase` | Words with highest error rate |
| `GetAccuracyByLevelUseCase` | Accuracy per learning level (0–6) |
| `GetBestStudyTimeUseCase` | Hour of day with best performance |
| `GetStudyHeatmapUseCase` | Calendar heatmap (last 30 days) |
| `GetWeeklyReportUseCase` | This week vs. last week comparison |
| `GetAccuracyByDayOfWeekUseCase` | Accuracy per weekday |
| `GetLevelTransitionsUseCase` | Progression between levels |
| `GetResponseTimeTrendUseCase` | Speed improvements over time |

Analytics split across two repository interfaces: `IAnalyticsRecorder` (write) and `IAnalyticsStatsRepository` + `IAnalyticsWordRepository` (read).

---

### 6. Profile & Streaks

**ProfileStats**: `currentStreak`, `longestStreak`, `memberSince`, `weeklyActivity` (List<DayActivity>), `languages` (List<LanguagePair>).

**Avatar**: Upload/delete via `UploadAvatarUseCase` / `DeleteAvatarUseCase`.

**Streaks**: `GetStreakUseCase`, `RecordStreakActivityUseCase` (called on session completion).

---

### 7. Settings

All stored locally via `ISettingsRepository`:

| Setting | Type | Notes |
|---------|------|-------|
| App language | String | UI language |
| Theme mode | Light / Dark / System | |
| Notifications enabled | Boolean | Master toggle |
| Review reminders | Boolean | |
| Motivational messages | Boolean | |
| Daily reminder time | Time | |
| Min due cards threshold | Int | Minimum before reminder fires |
| Review difficulty | successesToAdvance (1–3), forgotPenalty (1–3) | |
| TTS speech rate | Float 0.5–2.0 | |
| Per-language TTS voice | Map<lang, voice> | |

---

### 8. Text-to-Speech (TTS)

**States**: Idle → Downloading(code, progress) / Loading / Speaking → Idle or Error

**Operations**: `SpeakWordUseCase`, `DownloadTtsModelUseCase`, `DeleteTtsModelUseCase`, `GetTtsModelsInfoUseCase`

Language models are downloaded on demand. Each platform uses its own engine:
- Android: on-device TTS engine
- iOS: AVFoundation
- WASM: Web Speech API

Per-language voice selection and global speech rate (0.5–2.0×).

---

### 9. Notifications

**Types**: Review reminders, motivational messages, push via FCM (Android) / APNs (iOS).

**Setup flow**: `InitializePushNotificationsUseCase` → `RequestNotificationPermissionUseCase` → `RegisterPushTokenUseCase` (sends token to backend) → `ScheduleNotificationsUseCase`.

---

### 10. Leaderboard

Global ranking by streak / mastered words. Each `LeaderboardEntry`: rank, displayName, currentStreak, longestStreak, masteredWords, isCurrentUser, profileImageUrl.

---

### 11. Subscriptions (RevenueCat)

**Statuses**: FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED (stored in `AuthUser.subscriptionStatus`)
**Plans**: Monthly, Annual, Lifetime
**Feature access**: `GetFeatureAccessUseCase` → `FeatureFlags` (e.g., `pushNotificationsEnabled`)

---

### 12. Onboarding

Collected: `targetLanguage`, `nativeLanguage`, `level`, `interests` → submitted via `SubmitPreferencesUseCase` → backend returns suggested vocabulary → user previews/approves → `ImportSuggestedVocabularyUseCase`.

Onboarding is skipped if user already has words in the local DB.

---

## Navigation Structure

**Main navigation**: Bottom tabs — Study | Settings

**Feature graphs**: Profile, Subscription, Insights

**App-level states** (AppNavigationViewModel):
- `Verifying` → `LoginRequired` → `Onboarding` → `VocabularyPreview` → `Ready`

**Key file**: `presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt`

---

## Backend API Surface (Summary)

| Domain | Endpoints |
|--------|-----------|
| Words | GET/POST/PUT/DELETE `/words`, PATCH `/words/languages`, DELETE batch |
| Auth | POST `/auth/login/{google,apple}`, POST `/auth/logout`, POST `/auth/verify-session`, DELETE `/auth/account` |
| Analytics | POST `/analytics/sessions/{start,end}`, POST `/analytics/events`; GET `/analytics/{insights,stats/*,words/*}` |
| Profile | GET/PUT `/profile`, POST/DELETE `/profile/avatar`, GET `/profile/stats` |
| Notifications | POST `/notifications/register-token`, POST `/notifications/schedule` |
| Leaderboard | GET `/leaderboard` |
| Features | GET `/features/access` |
| Onboarding | POST `/onboarding/preferences`, GET `/onboarding/status` |
| Streaks | GET `/streaks`, POST `/streaks/activity` |
| AI | POST `/ai/extract-vocabulary` |

---

## Progress Evaluation Tiers

`EvaluateProgressUseCase` maps `progressFraction` (0.0–1.0) to a tier:

`EMPTY → GETTING_STARTED → BUILDING → PROGRESSING → HALFWAY → STRONG → ALMOST_MASTER → MASTERED`

---

## DI Entry Point

All modules registered in:
`composeApp/src/commonMain/kotlin/di/AppModule.kt`

Analytics split: `AnalyticsModule.kt` in same package.

---

## What Claude Should Know Before Touching Any Feature

1. **Study flow** touches `ReviewViewModel`, `StudyProgressViewModel`, `ReviewWordUseCase`, `IWordRepository`, `IAnalyticsRecorder` — all must stay in sync.
2. **Analytics** has a **write side** (`IAnalyticsRecorder` / `AnalyticsRecorderImpl`) and a separate **read side** (`IAnalyticsStatsRepository`, `IAnalyticsWordRepository`). Do not conflate them.
3. **Auth state** drives the entire app shell — changes to `AppNavigationViewModel` affect all feature entry points.
4. **Word model level field** is the SR bucket (0–6), not a UI display level — arithmetic on it has direct learning consequences.
5. **Sync** treats backend as source of truth. Local DB is primary for offline reads; writes are queued and synced.
6. **TTS state** is a `StateFlow` on `ITtsRepository` — UI observes it to show download progress, speaking indicator, etc.
7. **FeatureFlags** gate premium features — check `GetFeatureAccessUseCase` before adding features gated on subscription.
