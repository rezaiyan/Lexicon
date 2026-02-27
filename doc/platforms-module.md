# Platforms Module

Platform-specific bridges in `platforms/src/`. Uses expect/actual pattern.

## Interfaces (commonMain)

### SecureStorage (`data/storage/SecureStorage.kt`)
```kotlin
interface SecureStorage {
    suspend fun saveAccessToken(token: String)
    suspend fun saveRefreshToken(token: String)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun saveTokenExpiresAt(expiresAt: Long)
    suspend fun getTokenExpiresAt(): Long
    suspend fun storeDailyInsightData(insightId: String, date: String)
    suspend fun getDailyInsightData(): DailyInsightData?
    suspend fun clearDailyInsightData()
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}
```

### INotificationManager (`notification/INotificationManager.kt`)
- `areNotificationsEnabled()`, `requestNotificationPermission()`, `wasNotificationPermissionDenied()`
- `scheduleReviewReminder()`, `scheduleMotivationalNotification()`
- `cancelAllNotifications()`, `showImmediateNotification()`, `clearBadge()`
- `openNotificationSettings()`

### ITtsEngine (`tts/ITtsEngine.kt`)
- `initialize(modelPath, tokensPath, dataDir)`, `synthesizeAndPlay(text)`, `stop()`, `release()`, `isInitialized()`

### IModelFileManager (`tts/IModelFileManager.kt`)
- `isModelPresent()`, `downloadAndExtractModel(): Flow<Float>`, `getModelFilePath()`, `getTokensFilePath()`, `getDataDir()`, `deleteModelFiles()`

### IAnalyticsTracker (`analytics/IAnalyticsTracker.kt`)
- logScreenView, logEvent, logWordReviewed, logWordMastered
- logReviewSessionStart/Complete, logWordsImported
- logStreakUpdated, logDailyGoalCompleted, logAiInsightGenerated
- logThemeChanged, logLanguageChanged
- setUserProperty, updateUserProgress, logError, logNonFatalError

### IGoogleAuthStateProvider (`auth/IGoogleAuthStateProvider.kt`)
- `isSignedInWithGoogle()`, `getSilentGoogleIdToken()`, `signOutFromGoogle()`

### IAppleAuthStateProvider (`auth/IAppleAuthStateProvider.kt`)
- `isSignedInWithApple()`, `getAppleUserIdentifier()`, `signOutFromApple()`

### IPushTokenManager (`pushnotification/IPushTokenManager.kt`)
- `initialize(onTokenReceived)`, `getCurrentToken()`

### IAppVersionProvider (`platform/IAppVersionProvider.kt`)
- `getAppVersion(): String`

## Android Implementations

| Interface | Implementation | Backend |
|-----------|---------------|---------|
| SecureStorage | `AndroidSecureStorage` | EncryptedSharedPreferences (MasterKey AES256_GCM) |
| INotificationManager | `AndroidNotificationManager` | NotificationCompat + AlarmManager |
| ITtsEngine | `AndroidTtsEngine` | Sherpa ONNX + AudioTrack |
| IModelFileManager | `AndroidModelFileManager` | Apache Commons Compress (BZ2/Tar) |
| IAnalyticsTracker | `AndroidAnalyticsTracker` | Firebase Analytics + Crashlytics |
| IGoogleAuthStateProvider | `AndroidGoogleAuthStateProvider` | Firebase Auth |
| IAppleAuthStateProvider | `AndroidAppleAuthStateProvider` | No-op stub |
| IPushTokenManager | `AndroidPushTokenManager` | FirebaseMessaging |
| IAppVersionProvider | `AndroidAppVersionProvider` | PackageManager |

## iOS Implementations

| Interface | Implementation | Backend |
|-----------|---------------|---------|
| SecureStorage | `IOSKeychainSecureStorage` | iOS Keychain (Security framework) + NSUserDefaults |
| INotificationManager | `IosNotificationManager` | UNUserNotificationCenter |
| ITtsEngine | `IosTtsEngine` | Sherpa ONNX + AVAudioPlayer |
| IModelFileManager | `IosModelFileManager` | NSData + bz2lib + custom tar parser |
| IAnalyticsTracker | `IOSAnalyticsTracker` | NSLog (Firebase in Swift layer) |
| IGoogleAuthStateProvider | `IOSGoogleAuthStateProvider` | Firebase Auth |
| IAppleAuthStateProvider | `IOSAppleAuthStateProvider` | NSUserDefaults + Apple Auth Services |
| IPushTokenManager | `IOSPushTokenManager` | Static callback bridge from Swift |
| IAppVersionProvider | `IOSAppVersionProvider` | NSBundle Info.plist |

## WasmJs Implementations
All are minimal stubs/no-ops. Notable:
- `WasmJsSecureStorage`: Uses `localStorage`
- `WasmJsNotificationManager`: Uses browser Notification API
- TTS: Not available on web

## Factory Pattern
Each platform feature uses an expect/actual factory function:
```kotlin
// commonMain
expect fun createNotificationManager(): INotificationManager

// androidMain
actual fun createNotificationManager(): INotificationManager = AndroidNotificationManager(...)

// iosMain
actual fun createNotificationManager(): INotificationManager = IosNotificationManager()
```
