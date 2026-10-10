package com.alirezaiyan.vokab

import account.IOSAccountDeletionHandler
import androidx.compose.ui.window.ComposeUIViewController
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import config.AppConfig
import di.appModule
import di.iosPlatformModule
import di.mobileModule
import domain.auth.repository.IAuthRepository
import domain.notifications.usecase.ReportNotificationOpenedUseCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import notification.NotificationCategory
import notification.NotificationTapHandler
import notification.payload.NotificationPayloadHandlerRegistry
import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.Foundation.NSLog
import presentation.ui.LexiconApp
import pushnotification.IOSPushTokenManager
import widget.IosWidgetUpdater

object NotificationCategoryConstants {
    const val STREAK_REMINDER = "STREAK_REMINDER"
    const val REVIEW_REMINDER = "REVIEW_REMINDER"
    const val GENERIC = "GENERIC"

    const val TYPE_STREAK_REMINDER = "streak_reminder"
    const val TYPE_REVIEW_REMINDER = "review_reminder"
}

private var accountDeletionHandler: IOSAccountDeletionHandler? = null
private var koinInstance: Koin? = null
private var googleAuthInitialized = false
private var revenueCatInitialized = false

fun MainViewController() = ComposeUIViewController {
    initializeDependencies()
    LexiconApp()
}

fun warmup() {
    initializeDependencies()
}

private fun initializeDependencies() {
    if (!googleAuthInitialized) {
        GoogleAuthProvider.create(
            credentials = GoogleAuthCredentials(
                serverId = AppConfig.GOOGLE_SERVER_CLIENT_ID
            )
        )
        NSLog("GoogleAuthProvider initialized")
        googleAuthInitialized = true
    }
    if (!revenueCatInitialized) {
        Purchases.logLevel = LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration(AppConfig.REVENUECAT_IOS_KEY)
        )
        NSLog("RevenueCat initialized")
        revenueCatInitialized = true
    }
    startKoinIfNeeded()
}

private fun startKoinIfNeeded() {
    if (koinInstance == null) {
        val koinApplication = startKoin {
            modules(
                iosPlatformModule(),
                mobileModule(),
                appModule(
                    backendUrl = AppConfig.VOKAB_BACKEND_URL,
                    platform = data.notification.remote.model.Platform.IOS
                )
            )
        }
        koinInstance = koinApplication.koin
    }
    if (accountDeletionHandler == null) {
        koinInstance?.let { accountDeletionHandler = it.get() }
    }

    // Update widget data on app launch
    koinInstance?.let { IosWidgetUpdater.update(it) }
}

fun clearUserData() {
    startKoinIfNeeded()
    accountDeletionHandler?.clearUserData() ?: run {
        val handler: IOSAccountDeletionHandler = koinInstance?.get() 
            ?: throw IllegalStateException("Koin not initialized")
        handler.clearUserData()
        accountDeletionHandler = handler
    }
}

/**
 * Called from Swift's didReceive with the push's notification_log_id (absent for local
 * notifications), type and deep_link (together decide which screen the tap opens).
 */
fun notifyNotificationTapped(notificationLogId: String?, type: String?, deepLink: String?) {
    val data = buildMap {
        notificationLogId?.let { put(ReportNotificationOpenedUseCase.NOTIFICATION_LOG_ID_KEY, it) }
        type?.let { put(NotificationTapHandler.TYPE_KEY, it) }
        deepLink?.let { put(NotificationTapHandler.DEEP_LINK_KEY, it) }
    }
    if (data.isEmpty()) return
    startKoinIfNeeded()
    koinInstance?.get<NotificationTapHandler>()?.onNotificationTapped(data)
}

/**
 * Called from Swift for a silent (content-available) push. [onComplete] runs once the work is
 * done, so Swift can call the background fetch completion handler only then.
 */
fun handleSilentPush(type: String?, onComplete: () -> Unit) {
    startKoinIfNeeded()
    val registry = koinInstance?.get<NotificationPayloadHandlerRegistry>()
    if (registry == null) {
        onComplete()
        return
    }
    MainScope().launch {
        registry.handleAndAwait(type, emptyMap())
        onComplete()
    }
}

fun notifyPushTokenReceived(token: String) {
    IOSPushTokenManager.notifyTokenReceived(token)
}

fun shouldShowNotification(categoryValue: String?): Boolean {
    startKoinIfNeeded()
    val category = NotificationCategory.fromString(categoryValue)
    return runBlocking {
        when (category) {
            NotificationCategory.USER -> {
                val authRepository: IAuthRepository = koinInstance?.get()
                    ?: return@runBlocking false
                authRepository.isAuthenticated()
            }
            NotificationCategory.SYSTEM -> true
        }
    }
}

