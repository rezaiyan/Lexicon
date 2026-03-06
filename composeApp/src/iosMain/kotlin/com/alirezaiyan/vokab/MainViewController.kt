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
import kotlinx.coroutines.runBlocking
import notification.NotificationCategory
import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.Foundation.NSLog
import presentation.ui.LexiconApp
import pushnotification.IOSPushTokenManager

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
    if (accountDeletionHandler == null && koinInstance != null) {
        accountDeletionHandler = koinInstance!!.get()
    }
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

