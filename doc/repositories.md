# Repositories

Interfaces in `domain/`, implementations in `data/`.

## IAuthRepository
**Interface**: `domain/auth/repository/IAuthRepository.kt`
**Impl**: `data/auth/repository/AuthRepositoryImpl.kt`
**Deps**: ITokenManager, ISessionManager, FeatureAccessRemoteDataSource, AuthDataSource, IGoogleAuthStateProvider, IAppleAuthStateProvider

| Method | Return | Notes |
|--------|--------|-------|
| `loginWithGoogle(idToken)` | `Try<AuthUser>` | Google OAuth, saves tokens, sets authenticated |
| `loginWithApple(idToken, fullName?, appleUserId)` | `Try<AuthUser>` | Apple Sign-In, saves tokens |
| `logout()` | `Try<Unit>` | Clears tokens, signs out Google/Apple providers |
| `deleteAccount()` | `Try<Unit>` | DELETE to backend, then clears local |
| `getAccessToken()` | `String?` | From secure storage |
| `isAuthenticated()` | `Boolean` | Checks session state |
| `isAuthenticatedAsFlow()` | `Flow<Boolean>` | Reactive auth state |
| `getFeatureAccessAsFlow()` | `Flow<FeatureAccessResponse>` | Premium/feature flags from backend |

## ISessionRepository
**Interface**: `domain/auth/repository/ISessionRepository.kt`
**Impl**: `data/session/repository/SessionRepositoryImpl.kt`
**Deps**: AuthDataSource, SecureStorage

| Method | Return | Notes |
|--------|--------|-------|
| `verifySession()` | `SessionVerificationResult` | Calls GET /users/me to verify token. Returns Valid/Expired/NotAuthenticated/ServerError |

## IWordRepository
**Interface**: `domain/word/repository/IWordRepository.kt`
**Impl**: `data/word/repository/WordRepositoryImpl.kt`
**Deps**: IWordLocalDataSource, IWordRemoteSyncHandler, IWordConflictResolver

| Method | Return | Notes |
|--------|--------|-------|
| `getAllWordsAsync()` | `List<Word>` | Suspend, one-shot |
| `getAllWords()` | `Flow<List<Word>>` | Reactive |
| `getDueCards()` | `Flow<List<Word>>` | nextReviewDate <= now |
| `getWordsByStage(stage)` | `Flow<List<Word>>` | Filter by level |
| `getWordById(id)` | `Word?` | Single word lookup |
| `insertWords(words)` | `Int` | Dedup + insert + sync to remote. Returns inserted count |
| `updateWord(word)` | Unit | Update locally + sync to remote |
| `deleteWord(id)` | Unit | Delete locally + sync to remote |
| `deleteWords(ids)` | `Flow<DeleteWordsProgress>` | Batch delete with progress |
| `deleteAllWords()` | `Try<Unit>` | Clear all |
| `syncWithRemote()` | `Try<Unit>` | Bidirectional sync with conflict resolution |
| `syncRemoteToLocal(clearFirst)` | `Try<Unit>` | Pull from remote |
| `getProgressStats()` | `Flow<ProgressStats>` | Aggregate counts |
| `getTotalCount()` | `Int` | Total words |
| `getDueCount()` | `Int` | Due cards count |

## ISettingsRepository
**Interface**: `domain/settings/repository/ISettingsRepository.kt`
**Impl**: `data/settings/repository/SettingsRepositoryImpl.kt`
**Deps**: LexiconQueries (SQLDelight)

Local-only repository (no remote sync). Settings stored in SQLDelight singleton row.

| Method | Return |
|--------|--------|
| `getLanguage()` | `Flow<Language>` |
| `setLanguage(language)` | Unit |
| `getThemeMode()` | `Flow<ThemeMode>` |
| `setThemeMode(mode)` | Unit |
| `getNotificationsEnabled()` | `Flow<Boolean>` |
| `setNotificationsEnabled(enabled)` | Unit |
| `getReviewRemindersEnabled()` | `Flow<Boolean>` |
| `setReviewRemindersEnabled(enabled)` | Unit |
| `getMotivationalMessagesEnabled()` | `Flow<Boolean>` |
| `setMotivationalMessagesEnabled(enabled)` | Unit |
| `getDailyReminderTime()` | `String` (HH:MM) |
| `setDailyReminderTime(time)` | Unit |
| `getMinimumDueCards()` | `Int` |
| `setMinimumDueCards(count)` | Unit |
| `clearSettings()` | Unit |

## IStreakRepository
**Interface**: `domain/streak/repository/IStreakRepository.kt`
**Impl**: `data/streak/repository/StreakRepositoryImpl.kt`
**Deps**: StreakRemoteDataSource

| Method | Return |
|--------|--------|
| `getStreak()` | `Try<StreakData>` |
| `recordActivity()` | `Try<StreakData>` |

## IAiRepository
**Interface**: `domain/ai/repository/IAiRepository.kt`
**Impl**: `data/ai/repository/AiRepositoryImpl.kt`
**Deps**: AiRemoteDataSource

| Method | Return | Notes |
|--------|--------|-------|
| `extractVocabularyFromImage(imageBytes, targetLanguage, extractWords, extractSentences)` | `Try<String>` | Max 3MB image, base64 encoded |

## ITtsRepository
**Interface**: `domain/tts/repository/ITtsRepository.kt`
**Impl**: `data/tts/repository/TtsRepositoryImpl.kt`
**Deps**: ITtsEngine, IModelFileManager, LanguageModelMapping

| Method | Return | Notes |
|--------|--------|-------|
| `ttsState` | `StateFlow<TtsState>` | IDLE/Loading/Speaking/Downloading/Error |
| `speak(text, languageCode)` | Unit | Load model if needed, synthesize + play |
| `stop()` | Unit | Stop playback |
| `isModelDownloaded(languageCode)` | `Boolean` | Check local model presence |
| `downloadModel(languageCode)` | `Flow<Float>` | Download sherpa-onnx model, emit progress |
| `isLanguageSupported(languageCode)` | `Boolean` | Check model availability |

## INotificationRepository
**Interface**: `domain/notifications/repository/INotificationRepository.kt`
**Impl**: `data/notification/repository/NotificationRepositoryImpl.kt`
**Deps**: INotificationManager

| Method | Return |
|--------|--------|
| `scheduleReviewReminder(dueCount, title, message, delayMinutes)` | Unit |
| `areNotificationsEnabled()` | `Boolean` |
| `requestNotificationPermission()` | `Boolean` |
| `wasNotificationPermissionDenied()` | `Boolean` |
| `openNotificationSettings()` | Unit |

## IPushTokenRepository
**Interface**: `domain/notifications/repository/IPushTokenRepository.kt`
**Impl**: `data/notification/repository/PushTokenRepositoryImpl.kt`
**Deps**: IPushTokenManager, PushNotificationDataSource

| Method | Return |
|--------|--------|
| `initializeAndRegister()` | Unit |
| `registerToken(token)` | `Try<Unit>` |
| `deactivateAllTokens()` | `Try<Unit>` |

## IOnboardingRepository
**Interface**: `domain/onboarding/repository/IOnboardingRepository.kt`
**Impl**: `data/onboarding/repository/OnboardingRepositoryImpl.kt`
**Deps**: OnboardingRemoteDataSource, SecureStorage

| Method | Return |
|--------|--------|
| `submitPreferences(preferences)` | `Try<SuggestedVocabularyResponse>` |
| `hasCompletedOnboarding()` | `Boolean` |
| `markOnboardingCompleted()` | Unit |

## Manager Interfaces

### IUserManager (`domain/auth/manager/`)
```kotlin
fun observeUser(): Flow<AuthUser?>
fun setUser(user: AuthUser?)
suspend fun logout(): Try<Unit>
suspend fun deleteAccount(): Try<Unit>
```

### ISessionManager (`domain/auth/session/`)
```kotlin
val isAuthenticatedFlow: StateFlow<Boolean>
suspend fun setAuthenticated(isAuthenticated: Boolean)
suspend fun isAuthenticated(): Boolean
fun initialize(scope: CoroutineScope)
```

### IStreakManager (`domain/streak/manager/`)
```kotlin
fun getStreak(): Flow<StreakState>  // Loading | Error | Loaded
suspend fun recordActivity(): Try<StreakData>
```

### ISubscriptionManager (`domain/subscription/`)
```kotlin
val customerInfo: StateFlow<SubscriptionCustomerInfo?>
suspend fun getOfferings(): Try<SubscriptionOffering>
suspend fun purchase(pkg): Try<SubscriptionCustomerInfo>
suspend fun restore(): Try<SubscriptionCustomerInfo>
fun isSubscribed(): Flow<Boolean>
suspend fun logIn(userId): Try<SubscriptionCustomerInfo>
suspend fun logOut(): Try<SubscriptionCustomerInfo>
suspend fun manageSubscription(): Try<Unit>
suspend fun cancelSubscription(): Try<Unit>
```
