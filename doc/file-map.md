# File Map

Key file paths organized by feature for quick navigation.

## Project Root
```
build.gradle.kts                    # Root build config
settings.gradle.kts                 # Module includes, repos
gradle/libs.versions.toml           # Centralized dependency versions
versioning.properties               # App version (versionCode=28, versionName=1.12.0)
local.defaults.properties           # Config template (backend URL, OAuth keys, RevenueCat)
local.properties                    # Actual config (gitignored)
scripts/bump-version.sh             # Version bumping script
scripts/sync-ios-config.sh          # iOS config sync
```

## Domain Module (`domain/src/commonMain/kotlin/domain/`)
```
common/Try.kt                      # Custom Result type

auth/model/AuthUser.kt              # AuthUser, AuthState, SubscriptionStatus
auth/model/FeatureAccess.kt         # FeatureFlags, UserFeatureAccess, FeatureAccessResponse
auth/repository/IAuthRepository.kt
auth/repository/ISessionRepository.kt
auth/service/AuthenticationService.kt
auth/manager/IUserManager.kt
auth/session/ISessionManager.kt
auth/storage/ISecureStorage.kt
auth/usecase/LoginWithGoogleUseCase.kt
auth/usecase/LoginWithAppleUseCase.kt
auth/usecase/LogoutUseCase.kt
auth/usecase/DeleteAccountUseCase.kt
auth/usecase/IsAuthenticatedUseCase.kt
auth/usecase/VerifySessionUseCase.kt
auth/usecase/ClearAllUserDataUseCase.kt
auth/usecase/GetFeatureAccessUseCase.kt

word/model/Word.kt                  # Word data class
word/model/ProgressStats.kt         # Level counts, due cards
word/model/LearningStage.kt         # 7-level enum
word/repository/IWordRepository.kt
word/service/WordSyncService.kt
word/service/ImportValidationService.kt
word/usecase/ReviewWordUseCase.kt   # SRS algorithm
word/usecase/ImportWordsUseCase.kt
word/usecase/ImportViaFileUseCase.kt
word/usecase/ExportWordsUseCase.kt
word/usecase/GetAllWordsUseCase.kt
word/usecase/GetDueWordsUseCase.kt
word/usecase/GetWordsByStageUseCase.kt
word/usecase/GetProgressStatsUseCase.kt
word/usecase/UpdateWordUseCase.kt
word/usecase/DeleteWordUseCase.kt
word/usecase/DeleteWordsUseCase.kt
word/usecase/SyncRemoteToLocalUseCase.kt

settings/model/ReviewSettings.kt    # SRS settings (presets)
settings/model/ThemeMode.kt
settings/repository/ISettingsRepository.kt
settings/usecase/GetCurrentLanguageUseCase.kt
settings/usecase/SetLanguageUseCase.kt
settings/usecase/GetReviewSettingsUseCase.kt
settings/usecase/SetThemeModeUseCase.kt
settings/usecase/SetNotificationsEnabledUseCase.kt

streak/model/StreakData.kt
streak/repository/IStreakRepository.kt
streak/manager/IStreakManager.kt
streak/usecase/GetStreakUseCase.kt
streak/usecase/RecordStreakActivityUseCase.kt

subscription/ISubscriptionManager.kt
subscription/model/SubscriptionModels.kt

tts/model/TtsState.kt
tts/repository/ITtsRepository.kt
tts/usecase/SpeakWordUseCase.kt
tts/usecase/StopSpeakingUseCase.kt

ai/repository/IAiRepository.kt
ai/usecase/ImportFromImageUseCase.kt
ai/usecase/IsAiAvailableUseCase.kt

notifications/repository/INotificationRepository.kt
notifications/repository/IPushTokenRepository.kt
notifications/usecase/ScheduleNotificationsUseCase.kt
notifications/usecase/RegisterPushTokenUseCase.kt
notifications/usecase/RequestNotificationPermissionUseCase.kt
notifications/usecase/OpenNotificationSettingsUseCase.kt
notifications/usecase/InitializePushNotificationsUseCase.kt

onboarding/model/OnboardingPreferences.kt
onboarding/model/SuggestedVocabulary.kt
onboarding/repository/IOnboardingRepository.kt
onboarding/usecase/SubmitPreferencesUseCase.kt
onboarding/usecase/ImportSuggestedVocabularyUseCase.kt
```

## Data Module (`data/src/commonMain/kotlin/data/`)
```
core/database/Lexicon.sq            # SQLDelight schema (2 tables, 20+ queries)
core/database/AppDatabase.kt
core/database/LexiconDao.kt
core/network/client/ApiClient.kt    # Try<T> HTTP wrapper
core/network/HttpClientProvider.kt  # Ktor factory
core/network/model/ApiResponse.kt
core/network/mapper/ApiResponseMapper.kt
core/network/interceptor/AuthInterceptor.kt
core/network/interceptor/ErrorInterceptor.kt
core/network/interceptor/RefreshAndRetryInterceptor.kt
core/network/error/NetworkExceptions.kt
core/network/error/HttpErrorMapper.kt
core/network/error/NetworkErrorHandler.kt

auth/repository/AuthRepositoryImpl.kt
auth/remote/AuthDataSource.kt       # 6 auth endpoints
auth/remote/FeatureAccessRemoteDataSource.kt
auth/remote/model/AuthModels.kt
auth/mapper/AuthMapper.kt
auth/token/TokenManager.kt
auth/refresh/TokenRefreshManager.kt
auth/state/AuthenticationStateManager.kt
auth/session/SessionManager.kt

word/repository/WordRepositoryImpl.kt
word/local/WordLocalDataSource.kt
word/remote/WordRemoteDataSource.kt  # 5 word endpoints
word/remote/model/WordRemoteModels.kt
word/mapper/WordMapper.kt
word/sync/WordRemoteSyncHandler.kt
word/sync/WordConflictResolver.kt

settings/repository/SettingsRepositoryImpl.kt

onboarding/repository/OnboardingRepositoryImpl.kt
onboarding/remote/OnboardingRemoteDataSource.kt
onboarding/remote/model/OnboardingModels.kt

streak/repository/StreakRepositoryImpl.kt
streak/remote/StreakRemoteDataSource.kt
streak/remote/model/StreakRemoteModels.kt

ai/repository/AiRepositoryImpl.kt
ai/remote/AiRemoteDataSource.kt
ai/remote/model/AiModels.kt

notification/repository/NotificationRepositoryImpl.kt
notification/repository/PushTokenRepositoryImpl.kt
notification/remote/PushNotificationDataSource.kt
notification/remote/model/PushNotificationModels.kt

session/repository/SessionRepositoryImpl.kt

tts/repository/TtsRepositoryImpl.kt
tts/LanguageModelMapping.kt         # 13 TTS language models

storage/SecureStorageAdapter.kt
subscription/RevenueCatSubscriptionManager.kt
```

## Presentation Module (`presentation/src/commonMain/kotlin/presentation/`)
```
ui/LexiconApp.kt                   # Main entry, nav graph, theme
ui/screens/SplashScreen.kt
ui/screens/OnboardingScreen.kt
ui/screens/VocabularyPreviewScreen.kt
ui/screens/AuthGateScreen.kt
ui/screens/StudyScreen.kt           # Main learning hub
ui/screens/ProfileScreen.kt
ui/screens/SettingsScreen.kt
ui/screens/settings/WordManagerScreen.kt

ui/screens/review/ReviewBottomSheet.kt
ui/screens/review/ReviewBottomSheetContent.kt
ui/screens/review/ReviewComponents.kt
ui/screens/review/DeckStackingAnimation.kt

ui/screens/study/StatsSection.kt
ui/screens/study/LearningStagesSection.kt
ui/screens/study/ProgressComponents.kt

ui/screens/subscription/SubscriptionScreen.kt
ui/screens/subscription/PlanCard.kt
ui/screens/subscription/ComparisonTable.kt

ui/components/imports/ImportBottomSheet.kt
ui/components/imports/ImportViewModel.kt
ui/components/imports/ImportUiState.kt
ui/components/imports/ImportEvent.kt
ui/components/imports/AiWordImportBottomSheet.kt
ui/components/imports/ImportMethodSelectorContent.kt

ui/components/FlashCard.kt
ui/components/LevelBucketCard.kt
ui/components/SettingsCard.kt
ui/components/LanguageSelectionDialog.kt
ui/components/ThemeModeDialog.kt
ui/components/NotificationDialogs.kt
ui/components/GoogleSignInContainer.kt
ui/components/AppleSignInButton.kt

ui/overlay/OverlayHost.kt
ui/overlay/dialog/DialogOverlay.kt
ui/overlay/bottomsheet/BottomSheetOverlay.kt

feature/auth/AuthViewModel.kt
feature/study/StudyViewModel.kt
feature/profile/ProfileViewModel.kt
feature/settings/SettingsViewModel.kt
feature/subscription/SubscriptionViewModel.kt
feature/onboarding/OnboardingViewModel.kt
feature/onboarding/VocabularyPreviewViewModel.kt
feature/aiimport/AiWordImportViewModel.kt

viewmodel/AppNavigationViewModel.kt
viewmodel/VocabularyViewModel.kt
viewmodel/WordManagerViewModel.kt

model/UiState.kt
model/DialogState.kt
model/ScreenStates.kt
model/WordManagerState.kt
model/ProfileUiData.kt
```

## ComposeApp Module (`composeApp/src/`)
```
commonMain/kotlin/di/AppModule.kt         # Root DI
commonMain/kotlin/di/NetworkModule.kt
commonMain/kotlin/di/AuthModule.kt
commonMain/kotlin/di/WordModule.kt
commonMain/kotlin/di/SettingsModule.kt
commonMain/kotlin/di/OnboardingModule.kt
commonMain/kotlin/di/TtsModule.kt
commonMain/kotlin/di/NotificationModule.kt
commonMain/kotlin/di/PresentationModule.kt
commonMain/kotlin/notification/NotificationHandler.kt
commonMain/kotlin/notification/NotificationFilter.kt
commonMain/kotlin/notification/payload/NotificationPayloadHandlerRegistry.kt
commonMain/kotlin/notification/payload/AccountDeletionHandler.kt
commonMain/kotlin/notification/payload/SignOutHandler.kt
commonMain/kotlin/notification/payload/DailyInsightHandler.kt

androidMain/kotlin/com/alirezaiyan/vokab/LexiconApplication.kt
androidMain/kotlin/com/alirezaiyan/vokab/MainActivity.kt
androidMain/kotlin/di/AndroidPlatformModule.kt

iosMain/kotlin/com/alirezaiyan/vokab/MainViewController.kt
iosMain/kotlin/di/IOSPlatformModule.kt
```

## Platforms Module (`platforms/src/`)
```
commonMain/kotlin/data/storage/SecureStorage.kt
commonMain/kotlin/auth/IGoogleAuthStateProvider.kt
commonMain/kotlin/auth/IAppleAuthStateProvider.kt
commonMain/kotlin/notification/INotificationManager.kt
commonMain/kotlin/tts/ITtsEngine.kt
commonMain/kotlin/tts/IModelFileManager.kt
commonMain/kotlin/analytics/IAnalyticsTracker.kt
commonMain/kotlin/pushnotification/IPushTokenManager.kt
commonMain/kotlin/platform/IAppVersionProvider.kt

androidMain/kotlin/data/storage/AndroidSecureStorage.kt
androidMain/kotlin/notification/AndroidNotificationManager.kt
androidMain/kotlin/tts/AndroidTtsEngine.kt
androidMain/kotlin/analytics/AndroidAnalyticsTracker.kt

iosMain/kotlin/data/storage/IOSKeychainSecureStorage.kt
iosMain/kotlin/notification/IosNotificationManager.kt
iosMain/kotlin/tts/IosTtsEngine.kt
iosMain/kotlin/analytics/IOSAnalyticsTracker.kt
```

## Tests
```
composeApp/src/commonTest/kotlin/domain/word/usecase/ReviewWordUseCaseTest.kt
composeApp/src/commonTest/kotlin/domain/word/usecase/ImportWordsUseCaseTest.kt
composeApp/src/commonTest/kotlin/domain/word/usecase/ExportWordsUseCaseTest.kt
composeApp/src/commonTest/kotlin/domain/word/service/ImportValidationServiceTest.kt
composeApp/src/commonTest/kotlin/domain/word/model/WordTest.kt
composeApp/src/commonTest/kotlin/domain/model/ReviewSettingsTest.kt
composeApp/src/androidInstrumentedTest/kotlin/.../EndToEndReviewTest.kt
composeApp/src/androidInstrumentedTest/kotlin/.../LexiconDaoTest.kt
```

## Design System
```
design-system/src/commonMain/kotlin/theme/AppTheme.kt  # Colors, spacing, typography
design-system/src/commonMain/kotlin/theme/PlatformFont.kt
```

## Utils
```
utils/src/commonMain/kotlin/utils/Language.kt          # 14 languages enum
utils/src/commonMain/kotlin/utils/StringFormatting.kt   # String.format()
utils/src/commonMain/kotlin/utils/ImageUtils.kt         # ByteArray.toImageBitmap()
utils/src/commonMain/kotlin/utils/CameraUtils.kt        # rememberCameraLauncher()
utils/src/commonMain/kotlin/utils/FilePickerCompose.kt  # rememberTextFilePickerLauncher()
```

## iOS App
```
iosApp/iosApp/ContentView.swift          # SwiftUI wrapper
iosApp/iosApp/iOSApp.swift               # App delegate, notifications
iosApp/Configuration/Config.xcconfig      # iOS build config
iosApp/Configuration/Config.private.xcconfig # Private keys (gitignored)
```

## CI/CD
```
.github/workflows/build.yml              # Build pipeline
.github/workflows/test.yml               # Test pipeline
.github/actions/init-config/action.yml   # Secret injection
```
