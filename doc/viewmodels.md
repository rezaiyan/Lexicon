# ViewModels

All ViewModels in `presentation/src/commonMain/kotlin/presentation/`.

## AppNavigationViewModel (`viewmodel/AppNavigationViewModel.kt`)
**State**: `appUiState: StateFlow<AppUiState>`

AppUiState values: Splash, Onboarding, VocabularyPreview, AuthGate, Ready

**Methods**:
- `onSplashComplete(isAuthenticated)` - Route to next screen
- `onNavigateToVocabularyPreview(words)` - After onboarding
- `onNavigateToAuthGate(pendingVocabulary)` - After preview
- `onAuthComplete()` - Import pending vocab, transition to Ready
- `onLogout()` - Back to AuthGate

## AuthViewModel (`feature/auth/AuthViewModel.kt`)
**State**: `authState: StateFlow<AuthState>`

**Intent-Based Pattern**:
```kotlin
sealed class AuthIntent {
    data class VerifyAndRestore(val onComplete: () -> Unit)
    data class LoginWithIdToken(val idToken: String)
    data class LoginWithApple(val idToken: String, val fullName: String?, val appleUserId: String)
    data object Logout
}
```

**Methods**: `verifyAndRestoreSession(onComplete)`, `loginWithGoogle(idToken)`, `loginWithApple(...)`, `logout()`
**Integrations**: Session verify, analytics tracking, push notification init, subscription manager login

## StudyViewModel (`feature/study/StudyViewModel.kt`)
**States**:
- `progressScreenState: StateFlow<UiState<ProgressScreenState>>` - Stats with level counts
- `reviewScreenState: StateFlow<ReviewScreenState>` - Word list + review type
- `ttsState: StateFlow<TtsState>` - TTS playback state

**Methods**:
- `refreshStats()` - Reload progress
- `startReview()`, `startDueReview()` - Begin review session
- `loadWordsByStage(stage)`, `startStageReview(stage)` - Browse by level
- `reviewWord(word, quality)` - Submit review (0=forgot, 1=remembered)
- `updateWord(word)`, `deleteWord(wordId)` - Word management
- `onReviewSessionComplete()` - Record streak, schedule notifications
- `speakWord(text, languageCode)`, `stopSpeaking()` - TTS

## VocabularyViewModel (`viewmodel/VocabularyViewModel.kt`)
**Emits**: `events: Flow<VocabularyEffect>`, `uiMessages: Flow<UiMessage>`
**Methods**: `loadWords(reviewMode)`, `updateWord(word)`, `deleteWord(wordId, onDeleted)`

Effects: ImportSuccess, ImportError, ImageImportSuccess, ImageImportError, ReviewSessionComplete

## ProfileViewModel (`feature/profile/ProfileViewModel.kt`)
**State**: `state: StateFlow<UiState<ProfileUiData>>`

ProfileUiData combines: userInfo, streak, featureAccess, isSubscriptionsEnabled

**Events**:
```kotlin
sealed interface ProfileEvent {
    data object Logout
    data object DeleteAccount
    data object ClearError
}
```

## SettingsViewModel (`feature/settings/SettingsViewModel.kt`)
**State**: `settingsScreenState: StateFlow<SettingsScreenState>` (language, theme, notifications, appVersion)
**Dialog State**: `dialogState: StateFlow<DialogState>`

**Events**:
```kotlin
sealed class SettingsEvent {
    data class SetLanguage(val language: Language)
    data class SetThemeMode(val mode: ThemeMode)
    data class SetNotificationsEnabled(val enabled: Boolean)
    data object RequestNotificationPermission
    data object RefreshNotificationPermissionStatus
    data class ShowDialog(val dialogState: DialogState)
    data object DismissDialog
}
```

**Effects**: NotificationPermissionGranted, OpenSystemNotificationSettings

## WordManagerViewModel (`viewmodel/WordManagerViewModel.kt`)
**State**: `state: StateFlow<WordManagerScreenState>` (words, selectedIds, searchQuery, editingWord, isLoading)

**Events**:
```kotlin
sealed interface WordManagerEvent {
    data object ResetState
    data class ToggleWordSelection(val wordId: Int)
    data object SelectAll / DeselectAll
    data class UpdateSearchQuery(val query: String)
    data class StartEditingWord(val word: Word)
    data class UpdateWord(val word: Word)
    data object DeleteSelectedWords
    data object ShareWords
}
```

**Effects**: WordDeleted(count), WordUpdated(word), WordsShared(count, text, timestamp), ShareFailed, Error(message)
**Sub-handlers**: WordDeletionHandler, WordExportHandler, WordEditingHandler

## ImportViewModel (`ui/components/imports/ImportViewModel.kt`)
**State**: `state: ImportUiState` (tabs, selectedTab, textInputState, fileImportState, imageImportState)
Uses `@Composable` state function with `produceState` for dynamic tab visibility based on feature access.

**Events**: FileImportSuccessful, ImageImportSuccessful, Error

**Methods**: `selectTab()`, `updateWord()`, `updateTranslation()`, `addWord()`, `selectImage()`, `importImage()`, `importFile()`, `confirmImport()`, `selectSourceLanguage()`, `selectTargetLanguage()`

## AiWordImportViewModel (`feature/aiimport/AiWordImportViewModel.kt`)
**State**: `state: StateFlow<AiWordImportUiState>`
**Steps**: TARGET_LANG -> NATIVE_LANG -> LEVEL -> TOPICS -> PREVIEW

**Events**: ImportSuccess(count), Dismiss
**Methods**: `selectTargetLanguage()`, `selectNativeLanguage()`, `selectLevel()`, `toggleTopic()`, `toggleWordSelection()`, `nextStep()`, `previousStep()`, `submit()`, `importSelected()`, `reset()`

## OnboardingViewModel (`feature/onboarding/OnboardingViewModel.kt`)
**State**: `state: StateFlow<OnboardingUiState>` (currentStep, selectedLanguages, selectedLevel, interests, isLoading, error)
**Events**: NavigateToPreview(response), NavigateToMain
**Methods**: `selectTargetLanguage()`, `selectNativeLanguage()`, `selectLevel()`, `nextStep()`, `previousStep()`, `submit()`, `skip()`

## VocabularyPreviewViewModel (`feature/onboarding/VocabularyPreviewViewModel.kt`)
**State**: `state: StateFlow<VocabularyPreviewUiState>` (words list, selectedIndices)
**Methods**: `setWords(words)`, `proceedWithSelected()`, `skip()`

## SubscriptionViewModel (`feature/subscription/SubscriptionViewModel.kt`)
**States**: `state: StateFlow<UiState<SubscriptionData>>`, `uiState: StateFlow<SubscriptionUiState>` (isPurchasing, error, success)
**Methods**: `loadOfferings()`, `purchasePackage()`, `restorePurchases()`, `manageSubscription()`, `cancelSubscription()`, `retry()`, `clearError()`, `clearSuccess()`

## Generic UI State Pattern
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    data class Loaded<T>(val value: T) : UiState<T>()
}
// Extensions: .isLoading(), .isError(), .isLoaded(), .onLoaded { }
```

## State Builders
- **ProfileStateBuilder**: Combines user, streak, feature access flows
- **SettingsStateBuilder**: Combines language, theme, notifications, version flows
