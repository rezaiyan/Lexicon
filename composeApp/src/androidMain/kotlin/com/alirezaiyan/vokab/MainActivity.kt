package com.alirezaiyan.vokab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import domain.notifications.usecase.ReportNotificationOpenedUseCase.Companion.NOTIFICATION_LOG_ID_KEY
import notification.NotificationTapReporter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.ui.LexiconApp
import presentation.viewmodel.AppNavigationViewModel

class MainActivity : ComponentActivity() {

    private val appNavigationViewModel: AppNavigationViewModel by viewModel()
    private val notificationTapReporter: NotificationTapReporter by inject()

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
     * Both tap paths land here with the push data as extras: notifications we display in the
     * foreground (AndroidNotificationDisplayService) and ones the system tray shows while the
     * app is in the background (FCM puts the data payload on the launcher intent).
     */
    private fun reportNotificationTap(intent: Intent?) {
        val logId = intent?.getStringExtra(NOTIFICATION_LOG_ID_KEY) ?: return
        // Consume it so a configuration change re-delivering this intent doesn't report again
        intent.removeExtra(NOTIFICATION_LOG_ID_KEY)
        notificationTapReporter.onNotificationTapped(mapOf(NOTIFICATION_LOG_ID_KEY to logId))
    }
}
