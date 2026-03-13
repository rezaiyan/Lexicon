# Notification System

## Architecture

```
Push Notification (FCM/APNS)
    │
    ├── Android: LexiconFirebaseMessagingService.onMessageReceived()
    │    └── Extracts RemoteMessage → NotificationData
    │
    └── iOS: UNUserNotificationCenter (Swift layer)
         └── Bridges to Kotlin

    ↓

NotificationHandler.processNotificationAsync()
    │
    ├── NotificationFilter.shouldShowNotification(category, authRepository)
    │    ├── USER category: only show if authenticated
    │    └── SYSTEM category: always show
    │
    ├── (if should show) → NotificationDisplayService.showNotification()
    │
    └── (always) → NotificationPayloadHandlerRegistry.handle(type, data)
         ├── "account_deleted" → AccountDeletionHandler → clearAllUserData()
         ├── "sign_out"       → SignOutHandler → clearAllUserData()
         ├── "streak_reminder"     → NoOpHandler
         ├── "review_reminder"     → NoOpHandler
         └── "achievement_unlocked" → NoOpHandler
```

## Notification Data
```kotlin
data class NotificationData(
    val title: String?,
    val body: String?,
    val data: Map<String, String>,
    val category: NotificationCategory,  // USER or SYSTEM
    val type: String?
)
```

## Local Notifications

Scheduled via `ScheduleNotificationsUseCase`:
- Triggers when review session completes
- Checks: notifications enabled AND dueCards >= minimumDueCards setting
- Schedules 24-hour reminder via `INotificationManager.scheduleReviewReminder()`

## Push Token Flow

1. App init → `InitializePushNotificationsUseCase()`
2. Checks if authenticated
3. Calls `RegisterPushTokenUseCase.initializeAndRegister()`
4. Platform manager gets FCM/APNS token
5. POST `/notifications/register-token` with `{token, platform, deviceId?}`
6. On logout: `deactivateAllTokens()` → DELETE `/notifications/tokens`

## Platform Details

### Android
- `LexiconFirebaseMessagingService` extends FirebaseMessagingService
- Notification channel: "lexicon_notifications" (HIGH importance, lights, vibration)
- `AndroidNotificationManager`: NotificationCompat + AlarmManager
- Badge increment on notification

### iOS
- `IosNotificationManager`: UNUserNotificationCenter
- Notification categories with action identifiers
- Badge management via `UIApplication.setApplicationIconBadgeNumber()`
- Permission: Alert + Sound + Badge
- Opens settings via "app-settings:" URL scheme

### Web
- Browser Notification API via `js()` calls
- No scheduling (would need Service Worker)

## Notification Categories
```kotlin
enum class NotificationCategory { USER, SYSTEM }
```

## iOS-Specific Notification Constants
Defined in `MainViewController.kt`:
- Category identifiers for review reminders, motivational messages
- Action identifiers for user interactions
