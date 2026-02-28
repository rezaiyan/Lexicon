package notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.R as AndroidR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class AndroidNotificationManager(
    private val context: Context
) : INotificationManager {
    
    companion object {
        private const val CHANNEL_ID = "lexicon_notifications"
        private const val REVIEW_NOTIFICATION_ID = 1001
        private const val MOTIVATIONAL_NOTIFICATION_ID = 1002
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vokab Learning Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friendly reminders to practice your vocabulary"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override suspend fun areNotificationsEnabled(): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    override suspend fun wasNotificationPermissionDenied(): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                false
            } else {
                val activity = context.findActivity()
                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } ?: false
            }
        } else {
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    private fun Context.findActivity(): android.app.Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
    
    override suspend fun requestNotificationPermission(): Boolean {
        // For Android 13+, we need to trigger the system permission dialog
        // This will be handled through an Intent to open system settings
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if already granted
            val alreadyGranted = areNotificationsEnabled()
            if (alreadyGranted) {
                true
            } else {
                // Open system settings for the app so user can enable notifications
                try {
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    false // Return false since we're navigating to settings
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            // Pre-Android 13, check if notifications are enabled in system settings
            val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!enabled) {
                // Open app settings
                try {
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            enabled
        }
    }
    
    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ) = withContext(Dispatchers.Main) {
        // Explicitly check permissions before scheduling
        if (!areNotificationsEnabled()) return@withContext
        
        try {
            // For immediate notifications (will implement WorkManager for scheduled ones)
            if (delayMinutes == 0) {
                showImmediateNotification(title, message)
            }
            // TODO: Implement WorkManager for delayed notifications
        } catch (e: SecurityException) {
            // Handle case where permission was revoked
            // Silently fail - don't crash the app
        }
    }
    
    override suspend fun scheduleMotivationalNotification(
        title: String,
        message: String,
        delayMinutes: Int
    ) = withContext(Dispatchers.Main) {
        // Explicitly check permissions before scheduling
        if (!areNotificationsEnabled()) return@withContext
        
        try {
            if (delayMinutes == 0) {
                showImmediateNotification(title, message)
            }
            // TODO: Implement WorkManager for delayed notifications
        } catch (e: SecurityException) {
            // Handle case where permission was revoked
            // Silently fail - don't crash the app
        }
    }
    
    override suspend fun showImmediateNotification(
        title: String,
        message: String
    ) = withContext(Dispatchers.Main) {
        // Explicitly check permissions before posting notification
        if (!areNotificationsEnabled()) return@withContext
        
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(AndroidR.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            // Check permission one more time before posting to handle SecurityException
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext
                }
            }
            
            NotificationManagerCompat.from(context).notify(REVIEW_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle case where permission was revoked between checks
            // Silently fail - don't crash the app
        }
    }
    
    override suspend fun cancelAllNotifications() = withContext(Dispatchers.Main) {
        try {
            // Canceling notifications doesn't require permission, but wrap in try-catch for safety
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: SecurityException) {
            // Handle edge case where something goes wrong
            // Silently fail - don't crash the app
        }
    }
    
    override suspend fun clearBadge() = withContext(Dispatchers.Main) {
        try {
            // On Android, badge count is typically managed by active notifications
            // Clear all notifications to clear the badge
            NotificationManagerCompat.from(context).cancelAll()
            println(" Badge cleared (Android)")
        } catch (e: SecurityException) {
            // Silently fail - don't crash the app
        }
    }
    
    override suspend fun openNotificationSettings() = withContext(Dispatchers.Main) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = "package:${context.packageName}".toUri()
                    }
                }
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            println(" Opened notification settings")
        } catch (e: Exception) {
            println(" Failed to open notification settings: ${e.message}")
        }
    }
}

