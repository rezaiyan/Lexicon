package notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.R as AndroidR

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TITLE = "notification_title"
        const val KEY_MESSAGE = "notification_message"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val TAG_REVIEW_REMINDER = "lexicon_review_reminder"
        const val TAG_MOTIVATIONAL = "lexicon_motivational"
        private const val CHANNEL_ID = "lexicon_notifications"
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 1001)

        ensureNotificationChannel()
        showNotification(title, message, notificationId)
        return Result.success()
    }

    private fun ensureNotificationChannel() {
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
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, message: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(AndroidR.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}
