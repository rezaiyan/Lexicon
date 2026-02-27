# Dependency Injection Setup

All DI modules in `composeApp/src/commonMain/kotlin/di/`. Uses Koin 4.1.1.

## Module Structure

```
appModule(backendUrl, platform)
├── networkModule(backendUrl)
├── authModule
├── wordModule
├── notificationModule
├── ttsModule
├── settingsModule
├── onboardingModule
└── presentationModule
```

Plus platform-specific modules:
- `androidPlatformModule` (composeApp/src/androidMain)
- `iosPlatformModule` (composeApp/src/iosMain)
- `mobileModule` (composeApp/src/mobileMain)

## AppModule (`di/AppModule.kt`)
Root aggregator (~21 lines). Takes `backendUrl` and `platform` parameters.
Includes all sub-modules.

## NetworkModule (`di/NetworkModule.kt`)
- `HttpClient` (singleton): Configured with auth/error interceptors
- `ApiResponseMapper` (singleton)
- `ApiClient` (singleton)
- Lazy evaluation for token refresh manager provider

## AuthModule (`di/AuthModule.kt`)
- **Storage**: ISecureStorage (via SecureStorageAdapter wrapping platform SecureStorage)
- **Token**: ITokenManager, IAuthenticationStateManager, ISessionManager
- **Refresh**: ITokenRefreshManager (with AuthDataSource + state manager)
- **Data sources**: AuthDataSource, FeatureAccessRemoteDataSource
- **Repositories**: IAuthRepository (AuthRepositoryImpl), ISessionRepository (SessionRepositoryImpl)
- **Services**: IAuthenticationService (AuthenticationService)
- **Use cases**: GetFeatureAccessUseCase, LoginWithGoogle/AppleUseCase, LogoutUseCase, DeleteAccountUseCase, IsAuthenticatedUseCase, VerifySessionUseCase, ClearAllUserDataUseCase

## WordModule (`di/WordModule.kt`)
- **Data sources**: WordLocalDataSource, WordRemoteDataSource, AiRemoteDataSource
- **Sync**: WordRemoteSyncHandler, WordConflictResolver
- **Repositories**: IWordRepository (WordRepositoryImpl), IAiRepository (AiRepositoryImpl)
- **Services**: IImportValidationService (ImportValidationService)
- **Use cases**: ReviewWordUseCase, ImportWordsUseCase, ImportFromImageUseCase, ImportViaFileUseCase, GetProgressStatsUseCase, GetWordsByStageUseCase, GetDueWordsUseCase, IsAiAvailableUseCase, SyncRemoteToLocalUseCase, GetAllWordsUseCase, DeleteWord/DeleteWordsUseCase, UpdateWordUseCase, ExportWordsUseCase

## SettingsModule (`di/SettingsModule.kt`)
- **Data sources**: StreakRemoteDataSource
- **Repositories**: ISettingsRepository (SettingsRepositoryImpl), IStreakRepository (StreakRepositoryImpl)
- **Use cases**: GetCurrentLanguageUseCase, GetReviewSettingsUseCase, SetLanguageUseCase, SetThemeModeUseCase, SetNotificationsEnabledUseCase, GetStreakUseCase, RecordStreakActivityUseCase

## OnboardingModule (`di/OnboardingModule.kt`)
- **Data source**: OnboardingRemoteDataSource
- **Repository**: IOnboardingRepository (OnboardingRepositoryImpl)
- **Use cases**: SubmitPreferencesUseCase, ImportSuggestedVocabularyUseCase

## TtsModule (`di/TtsModule.kt`)
- Creates TTS engine and model file manager via platform factories
- **Repository**: ITtsRepository (TtsRepositoryImpl)
- **Use cases**: SpeakWordUseCase, StopSpeakingUseCase

## NotificationModule (`di/NotificationModule.kt`)
- **Platform**: INotificationManager (platform factory), IPushTokenManager (platform factory)
- **Payload handlers registry** with 6 handlers:
  - AccountDeletionHandler (type: "account_deleted")
  - SignOutHandler (type: "sign_out")
  - DailyInsightHandler (type: "daily_insight")
  - NoOpHandler for: "streak_reminder", "review_reminder", "achievement_unlocked"
- **Data sources**: PushNotificationDataSource
- **Repositories**: IPushTokenRepository, INotificationRepository
- **Use cases**: ScheduleNotificationsUseCase, RegisterPushTokenUseCase, RequestNotificationPermissionUseCase, OpenNotificationSettingsUseCase, InitializePushNotificationsUseCase

## PresentationModule (`di/PresentationModule.kt`)
- **Platform**: IAnalyticsTracker (platform factory)
- **Managers**: IUserManager (UserManagerImpl), IStreakManager (StreakManagerImpl)
- **Notification permission monitor**
- **ViewModels** (13 total):
  1. AuthViewModel
  2. SettingsViewModel
  3. AppNavigationViewModel
  4. StudyViewModel
  5. ImportViewModel
  6. VocabularyViewModel
  7. WordManagerViewModel
  8. ProfileViewModel
  9. SubscriptionViewModel
  10. OnboardingViewModel
  11. VocabularyPreviewViewModel
  12. AiWordImportViewModel

## Platform Modules

### AndroidPlatformModule (`composeApp/src/androidMain/`)
- Room database driver factory
- Application context (singleton)
- AndroidSecureStorage (EncryptedSharedPreferences)
- AndroidAppVersionProvider
- AndroidGoogleAuthStateProvider, AndroidAppleAuthStateProvider (no-op)
- AndroidNotificationDisplayService

### IOSPlatformModule (`composeApp/src/iosMain/`)
- Room/SQLite database driver
- IOSKeychainSecureStorage
- IOSAppVersionProvider (Info.plist)
- IOSGoogleAuthStateProvider, IOSAppleAuthStateProvider
- IOSAccountDeletionHandler

### MobileModule (`composeApp/src/mobileMain/`)
- Shared mobile-specific registrations

## Initialization

**Android** (`LexiconApplication.kt`):
```kotlin
startKoin {
    androidContext(this@LexiconApplication)
    modules(androidPlatformModule, mobileModule, appModule(backendUrl, platform))
}
```

**iOS** (`MainViewController.kt`):
```kotlin
startKoin {
    modules(iosPlatformModule, mobileModule, appModule(backendUrl, platform))
}
```
