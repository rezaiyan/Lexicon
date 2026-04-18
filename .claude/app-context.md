# Lexicon App — Critical Functionality Reference

> Use before implementing any feature, fix, or refactor. Cross-reference with skills in `.claude/skills/` for implementation patterns.

---

## What the App Is

Lexicon = **offline-first, multilingual vocabulary learning app** for Android and iOS (KMP). Users add words, study via 7-level spaced repetition, track progress through analytics. Supports Google/Apple Sign-In, cloud sync, leaderboards, TTS, AI-powered import, subscriptions (RevenueCat), push notifications.

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

Study sessions tracked: start time, end time, cards reviewed, accuracy, completion status.

---

### 2. Authentication

**Providers**: Google OAuth, Apple Sign-In
**Storage**: JWT tokens in platform secure storage (Android Keystore / iOS Keychain)
**Subscription statuses**: FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED

**Use cases**: `LoginWithGoogleUseCase`, `LoginWithAppleUseCase`, `VerifySessionUseCase`, `LogoutUseCase`, `DeleteAccountUseCase`, `GetFeatureAccessUseCase`

On login: sync words from backend → init push notifications → init RevenueCat.

---

### 3. Word Management

**Word model fields**: `id`, `originalWord`, `translation`, `description`, `sourceLanguage`, `targetLanguage`, `level` (0–6), `easeFactor`, `interval`, `repetitions`, `lastReviewDate`, `nextReviewDate`, `dateAdded`, `tagIds` (List<Long>)

**Key operations**:
- CRUD: `GetAllWordsUseCase`, `UpdateWordUseCase`, `DeleteWordUseCase`, `DeleteWordsUseCase`
- Filtering: `GetDueWordsUseCase`, `GetWordsByStageUseCase`, `GetDueWordsByTagUseCase` (filter due cards by tag)
- Bulk: `BatchUpdateLanguagesUseCase`, `DeleteWordsUseCase` (with progress)
- Duplicate detection: `isSameContent()` — compares `originalWord` + `translation`
- Progress stats: `GetProgressStatsUseCase` → counts per level, due count

**Sync**: `SyncRemoteToLocalUseCase` (backend = source of truth on fresh sync)

**Tags**: Words carry `tagIds: List<Long>` populated by joining `WordTagEntity` at query time. Tag assignment separate — use `AssignWordTagsUseCase`.

---

### 3b. Tag System

Tags let users organise vocabulary and do focused study sessions on specific word groups.

**Domain model** (`domain/src/commonMain/kotlin/domain/tag/model/Tag.kt`):
```kotlin
data class Tag(val id: Long, val name: String, val wordCount: Long, val createdAt: Long, val updatedAt: Long)
```

**Database schema** (migration `5.sqm`):
- `TagEntity` — primary tag record
- `WordTagEntity (wordId, tagId)` — many-to-many junction table (composite PK)
- `wordCount` computed in SQL via `LEFT JOIN + COUNT()` on every tag read

**Repository**: `ITagRepository` (domain) / `TagRepositoryImpl` (data)
```
getTags(): Flow<List<Tag>>
createTag(name): Try<Tag>
renameTag(id, name): Try<Tag>
deleteTag(id): Try<Unit>           // cascades: deletes WordTagEntity rows first
assignWordTags(wordId, tagIds): Try<Unit>
addTagToWord(wordId, tagId): Try<Unit>
syncTagsFromRemote(): Try<Unit>
```

**Use cases** (all in `domain/src/commonMain/kotlin/domain/tag/usecase/`):
| Use case | Type | Purpose |
|----------|------|---------|
| `GetTagsUseCase` | `NoParamFlowUseCase<List<Tag>>` | Live tag list stream |
| `CreateTagUseCase` | `UseCase<String, Tag>` | Create new tag |
| `RenameTagUseCase` | `UseCase<RenameTagParams, Tag>` | Rename existing tag |
| `DeleteTagUseCase` | `UseCase<Long, Unit>` | Delete tag (cascades) |
| `AssignWordTagsUseCase` | `UseCase<AssignWordTagsParams, Unit>` | Atomically replace all tags on word |
| `GetDueWordsByTagUseCase` | `FlowUseCase<Long, List<Word>>` | Due cards filtered by tag |

**ViewModels** (in `feature/words/`):
- `TagManagerViewModel` — CRUD for tags; state: `TagManagerState(tags, isLoading, errorMessage)`
- `WordTagAssignmentViewModel` — tag assignment for single word; state: `WordTagAssignmentState(wordId, tags, selectedTagIds, isLoading, isSaving)`

**Screens/components** (in `presentation/`):
- `TagManagerScreen` — full-screen manager: create / rename / delete tags
- `TagAssignmentSheet` — bottom sheet to assign tags to word (accessed from `WordManagerDetailSheet`)
- `TagManagerCard` — settings card navigating to `TagManagerScreen`
- `TagsSection` — study progress screen component showing tags as clickable `LevelBucketCard` rows (each click launches tag-filtered review)

**Study integration**:
- `StudyProgressViewModel` subscribes to `GetTagsUseCase` → `state.tags` shown in `TagsSection`
- `ReviewViewModel.ReviewWordUseCases` includes `getDueWordsByTag` — clicking tag on Study screen launches review filtered to that tag's due words
- Word list flows (`getAllWords`, `getDueCards`) use `combine()` to re-emit when `WordTagEntity` rows change — tag assignment instantly refreshes all dependent screens

---

### 4. Import

Three import paths:

| Path | Use case | Description |
|------|----------|-------------|
| **File** | `ImportViaFileUseCase` | CSV / plain text, word-translation pairs |
| **AI Image** | `ImportFromImageUseCase` | Extract vocabulary from photo via AI |
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

Language models downloaded on demand. Each platform uses own engine:
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

Onboarding skipped if user already has words in local DB.

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
| Tags | GET `/tags`, POST `/tags`, PUT `/tags/{id}`, DELETE `/tags/{id}`, PUT `/words/{id}/tags` |
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

`EvaluateProgressUseCase` maps `progressFraction` (0.0–1.0) to tier:

`EMPTY → GETTING_STARTED → BUILDING → PROGRESSING → HALFWAY → STRONG → ALMOST_MASTER → MASTERED`

---

## DI Entry Point

All modules registered in:
`composeApp/src/commonMain/kotlin/di/AppModule.kt`

Analytics split: `AnalyticsModule.kt` in same package.

---

## What Claude Should Know Before Touching Any Feature

1. **Study flow** touches `ReviewViewModel`, `StudyProgressViewModel`, `ReviewWordUseCase`, `IWordRepository`, `IAnalyticsRecorder` — all must stay in sync.
2. **Analytics** has **write side** (`IAnalyticsRecorder` / `AnalyticsRecorderImpl`) and separate **read side** (`IAnalyticsStatsRepository`, `IAnalyticsWordRepository`). Don't conflate.
3. **Auth state** drives entire app shell — changes to `AppNavigationViewModel` affect all feature entry points.
4. **Word model level field** = SR bucket (0–6), not UI display level — arithmetic has direct learning consequences.
5. **Sync** treats backend as source of truth. Local DB primary for offline reads; writes queued and synced.
6. **TTS state** = `StateFlow` on `ITtsRepository` — UI observes to show download progress, speaking indicator, etc.
7. **FeatureFlags** gate premium features — check `GetFeatureAccessUseCase` before adding subscription-gated features.
8. **Tags** = cross-cutting concern on `Word`. Word list flows re-emit on `WordTagEntity` changes — any operation touching `WordTagEntity` fires all word list collectors. Keep tag-assignment transactional (`setWordTags` = atomic: delete-all then insert-all).
9. **Tag deletion** cascades synchronously in SQLDelight (delete `WordTagEntity` rows first, then `TagEntity`) — words unaffected, `tagIds` simply empty on next query.