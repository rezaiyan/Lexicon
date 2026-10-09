# Use Cases

All use cases in `domain/src/commonMain/kotlin/domain/`. Each is a standalone class with single responsibility.

## Auth Use Cases (`domain/auth/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `LoginWithGoogleUseCase` | IAuthenticationService | `(idToken: String): Flow<Try<AuthUser>>` | Google OAuth login |
| `LoginWithAppleUseCase` | IAuthenticationService | `(idToken, fullName?, appleUserId): Flow<Try<AuthUser>>` | Apple Sign-In |
| `LogoutUseCase` | IAuthenticationService, IWordRepository, ISettingsRepository | `(): Flow<Try<Unit>>` | Clears local words + settings, then calls logout API (succeeds even if API fails) |
| `DeleteAccountUseCase` | IAuthenticationService, IWordRepository, ISettingsRepository | `(): Flow<Try<Unit>>` | Deletes on server first, then clears local data |
| `IsAuthenticatedUseCase` | IAuthRepository | `(): Boolean` / `asFlow(): Flow<Boolean>` | Check auth state |
| `VerifySessionUseCase` | ISessionRepository | `(): SessionVerificationResult` | Verify token validity with backend |
| `ClearAllUserDataUseCase` | IWordRepository, ISettingsRepository, ISecureStorage, ISessionManager | `()` (suspend) | Nuclear clear: words, settings, tokens, session |
| `GetFeatureAccessUseCase` | IAuthRepository | `(): Flow<FeatureAccessResponse>` | Get premium/feature flags |

## Word Use Cases (`domain/word/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `GetAllWordsUseCase` | IWordRepository | `(): Flow<List<Word>>` | Reactive all words |
| `GetDueWordsUseCase` | IWordRepository | `(): Flow<List<Word>>` | Words due for review |
| `GetWordsByStageUseCase` | IWordRepository | `(stage: LearningStage): Flow<List<Word>>` | Words by bucket level |
| `GetProgressStatsUseCase` | IWordRepository | `(): Flow<ProgressStats>` | Aggregate stats (counts per level, due, total) |
| `ReviewWordUseCase` | IWordRepository, GetReviewSettingsUseCase | `(word, quality: Int)` (suspend) | **7-bucket SRS** - quality 0=forgot (drop N levels), quality 1=remembered (advance) |
| `UpdateWordUseCase` | IWordRepository | `(word): Try<Word>` (suspend) | Update word content, preserves learning progress |
| `DeleteWordUseCase` | IWordRepository | `(wordId: Int): Try<Unit>` (suspend) | Delete single word |
| `DeleteWordsUseCase` | IWordRepository | `(ids: List<Int>): Flow<DeleteWordsResult>` | Batch delete with progress states |
| `ExportWordsUseCase` | (none) | `(words): String` | Format: `word,translation[,description];...` |
| `SyncRemoteToLocalUseCase` | IWordRepository | `(clearFirst: Boolean): Try<Unit>` | Pull words from backend |

## Add Words Use Cases (`domain/word/add/usecase/`)

See `doc/import-export.md` for the pipeline.

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `AddWordsUseCase` | IWordRepository | `(AddWordsCommand): Try<AddWordsOutcome>` | Single write path: validate drafts, dedupe (term, translation, learning lang), link tags, enqueue upload |
| `AddStarterWordsUseCase` | AddWordsUseCase, IAddWordsLanguageRepository | `(List<SuggestedVocabulary>): Try<AddWordsOutcome>` | Onboarding starter words |
| `ParseWordFileUseCase` | (none) | `(WordFile): Try<ParseReport>` | Decode text (≤ 1 MB) and parse into drafts + rejected lines |
| `ExtractWordsFromImageUseCase` | IImagePreparer, IAiRepository, RefreshFeatureAccessUseCase | `(Params(image, languages, quarterTurns)): Try<List<WordDraft>>` | Turn as rotated, shrink to ≤ 3 MB, extract via `/ai/extract-words` |
| `SuggestWordsUseCase` | IAiRepository | `(Params(languages, level, topics)): Try<List<WordDraft>>` | AI suggestions |
| `ResolveAddWordsLanguagesUseCase` | IAddWordsLanguageRepository | `(): Try<LanguagePair?>` | Last used pair, else most common, else null |
| `UploadPendingWordsUseCase` | IWordRepository | `(): Try<Int>` | Flush the upload queue (called on app start) |

## Settings Use Cases (`domain/settings/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `GetReviewSettingsUseCase` | (none) | `(): ReviewSettings` | Returns BALANCED preset (fixed) |
| `SetThemeModeUseCase` | ISettingsRepository | `(mode: ThemeMode)` (suspend) | Set theme |
| `SetNotificationsEnabledUseCase` | ISettingsRepository | `(enabled: Boolean)` (suspend) | Toggle notifications |

## Streak Use Cases (`domain/streak/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `GetStreakUseCase` | IStreakRepository | `getUserStreaks(): Try<StreakData>` (suspend) | Fetch current streak from backend |
| `RecordStreakActivityUseCase` | IStreakRepository | `(): Try<StreakData>` (suspend) | Record activity, get updated streak |

## TTS Use Cases (`domain/tts/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `SpeakWordUseCase` | ITtsRepository | `(text, languageCode)` (suspend) | Normalize lang, download model if needed, speak; blank/unsupported code → skip (no guessed voice) |
| `StopSpeakingUseCase` | ITtsRepository | `()` (suspend) | Stop TTS playback |

## AI Use Cases (`domain/ai/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `IsAiAvailableUseCase` | IAuthRepository | `(): Boolean` (suspend) | Returns true if authenticated |

## Notification Use Cases (`domain/notifications/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `RequestNotificationPermissionUseCase` | INotificationRepository | `(): Boolean` (suspend) | Request OS notification permission |
| `ScheduleNotificationsUseCase` | INotificationRepository, ISettingsRepository | `(stats, titleProvider, messageProvider)` (suspend) | Schedule 24h reminder if enabled and dueCards >= minimum |
| `OpenNotificationSettingsUseCase` | INotificationRepository | `()` (suspend) | Open system notification settings |
| `InitializePushNotificationsUseCase` | IsAuthenticatedUseCase, RegisterPushTokenUseCase | `()` (suspend) | Register push token if authenticated |
| `RegisterPushTokenUseCase` | IPushTokenRepository | `initializeAndRegister()` / `registerToken(token)` / `deactivateAllTokens()` | FCM/APNS token management |

## Onboarding Use Cases (`domain/onboarding/usecase/`)

| Use Case | Dependencies | Signature | Behavior |
|----------|-------------|-----------|----------|
| `SubmitPreferencesUseCase` | IOnboardingRepository | `(preferences): Try<SuggestedVocabularyResponse>` (suspend) | Submit prefs, get vocab suggestions |

## Services (Domain Layer)

| Service | Interface | Implementation | Behavior |
|---------|----------|----------------|----------|
| `AuthenticationService` | IAuthenticationService | AuthenticationService | Wraps repository auth calls in Flow<Try<>> |
