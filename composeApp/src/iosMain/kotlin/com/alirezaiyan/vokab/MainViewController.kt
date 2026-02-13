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

/**
 * Constants for iOS notification categories and actions
 * These match the NotificationCategory enum in iOSApp.swift
 */
object NotificationCategoryConstants {
    // Notification category identifiers (must match Swift enum)
    const val ACCOUNT_DELETED = "ACCOUNT_DELETED"
    const val STREAK_REMINDER = "STREAK_REMINDER"
    const val REVIEW_REMINDER = "REVIEW_REMINDER"
    const val ACHIEVEMENT_UNLOCKED = "ACHIEVEMENT_UNLOCKED"
    const val GENERIC = "GENERIC"
    
    // Notification action identifiers
    const val ACTION_VIEW_PROGRESS = "VIEW_PROGRESS"
    const val ACTION_DISMISS = "DISMISS"
    const val ACTION_START_REVIEW = "START_REVIEW"
    const val ACTION_REMIND_LATER = "REMIND_LATER"
    const val ACTION_VIEW_ACHIEVEMENT = "VIEW_ACHIEVEMENT"
    
    // Notification type values (for userInfo["type"])
    const val TYPE_ACCOUNT_DELETED = "account_deleted"
    const val TYPE_STREAK_REMINDER = "streak_reminder"
    const val TYPE_REVIEW_REMINDER = "review_reminder"
    const val TYPE_ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
}

/**
 * Constants for NotificationCenter notification names
 * These are posted from Swift when notification actions are tapped
 */
object NotificationNameConstants {
    const val NAVIGATE_TO_PROGRESS = "NavigateToProgress"
    const val START_REVIEW_SESSION = "StartReviewSession"
    const val SHOW_ACHIEVEMENT_DETAILS = "ShowAchievementDetails"
    const val SCHEDULE_REVIEW_REMINDER = "ScheduleReviewReminder"
}

private var accountDeletionHandler: IOSAccountDeletionHandler? = null
private var koinInstance: Koin? = null
private var googleAuthInitialized = false
private var revenueCatInitialized = false

fun MainViewController() = ComposeUIViewController {
    initializeDependencies()
    LexiconApp()
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

fun isUserAuthenticated(): Boolean {
    startKoinIfNeeded()
    return runBlocking {
        val authRepository: IAuthRepository = koinInstance?.get()
            ?: return@runBlocking false
        authRepository.isAuthenticated()
    }
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

