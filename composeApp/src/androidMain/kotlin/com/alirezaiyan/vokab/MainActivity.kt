package com.alirezaiyan.vokab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import domain.notifications.usecase.ReportNotificationOpenedUseCase.Companion.NOTIFICATION_LOG_ID_KEY
import notification.NotificationTapHandler
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.ui.LexiconApp
import presentation.viewmodel.AppNavigationViewModel

class MainActivity : ComponentActivity() {

    private val appNavigationViewModel: AppNavigationViewModel by viewModel()
    private val notificationTapHandler: NotificationTapHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            appNavigationViewModel.isVerifying
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        reportNotificationTap(intent)
        setContent {
            LexiconApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reportNotificationTap(intent)
    }

    /**
     * Notification tap: reports the open and opens the screen the push is about. Both tap paths
     * land here with the push data as extras: notifications we display in the
     * foreground (AndroidNotificationDisplayService) and ones the system tray shows while the
     * app is in the background (FCM puts the data payload on the launcher intent).
     */
    private fun reportNotificationTap(intent: Intent?) {
        intent ?: return
        val data = listOf(NOTIFICATION_LOG_ID_KEY, NotificationTapHandler.TYPE_KEY)
            .mapNotNull { key -> intent.getStringExtra(key)?.let { key to it } }
            .toMap()
        if (data.isEmpty()) return
        // Consume them so a configuration change re-delivering this intent doesn't handle the tap again
        data.keys.forEach(intent::removeExtra)
        notificationTapHandler.onNotificationTapped(data)
    }
}
