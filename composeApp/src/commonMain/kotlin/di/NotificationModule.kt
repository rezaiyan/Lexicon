package di

import data.notification.remote.PushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.repository.NotificationRepositoryImpl
import data.notification.repository.PushTokenRepositoryImpl
import data.storage.SecureStorage
import domain.notifications.repository.INotificationRepository
import domain.notifications.repository.IPushTokenRepository
import domain.notifications.usecase.InitializePushNotificationsUseCase
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RegisterPushTokenUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.notifications.usecase.ScheduleNotificationsUseCase
import io.ktor.client.HttpClient
import notification.INotificationManager
import notification.createNotificationManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pushnotification.IPushTokenManager
import pushnotification.createPushTokenManager

fun notificationModule(backendUrl: String, platform: Platform) = module {

    // Notification Manager (platform-specific)
    single<INotificationManager> { createNotificationManager() }

    // Push Token Manager (platform-specific)
    single<IPushTokenManager> { createPushTokenManager() }

    // Notification Payload Handlers
    single<notification.payload.NotificationPayloadHandlerRegistry> {
        val handlers = listOf(
            notification.payload.AccountDeletionHandler(
                clearAllUserDataUseCase = get()
            ),
            notification.payload.SignOutHandler(
                clearAllUserDataUseCase = get()
            ),
            notification.payload.DailyInsightHandler(
                secureStorage = get()
            ),
            notification.payload.NoOpHandler("streak_reminder"),
            notification.payload.NoOpHandler("review_reminder"),
            notification.payload.NoOpHandler("achievement_unlocked"),
        ).associateBy { it.type }
        notification.payload.NotificationPayloadHandlerRegistry(handlers)
    }

    // Data Sources
    single {
        PushNotificationDataSource(
            baseUrl = backendUrl,
            getAuthToken = { get<SecureStorage>().getAccessToken() },
            httpClient = get<HttpClient>()
        )
    }

    // Repositories
    single<IPushTokenRepository> {
        PushTokenRepositoryImpl(
            pushTokenManager = get(),
            pushNotificationDataSource = get(),
            platform = platform
        )
    }

    single<INotificationRepository> {
        NotificationRepositoryImpl(notificationManager = get())
    }

    // Use Cases
    singleOf(::ScheduleNotificationsUseCase)
    singleOf(::RegisterPushTokenUseCase)
    singleOf(::RequestNotificationPermissionUseCase)
    singleOf(::OpenNotificationSettingsUseCase)
    singleOf(::InitializePushNotificationsUseCase)
}
