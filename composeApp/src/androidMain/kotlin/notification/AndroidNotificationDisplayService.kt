package notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alirezaiyan.vokab.MainActivity

class AndroidNotificationDisplayService(
    private val context: Context
) : NotificationDisplayService {
    
    companion object {
        private const val CHANNEL_GENERAL = "lexicon_notifications"
        private const val CHANNEL_REVIEW_REMINDERS = "review_reminders"
        private var NOTIFICATION_ID = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    override fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = when (data["type"]) {
            "review_reminder" -> CHANNEL_REVIEW_REMINDERS
            else -> CHANNEL_GENERAL
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID++, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Lexicon Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vocabulary learning reminders and updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(generalChannel)

            val reviewRemindersChannel = NotificationChannel(
                CHANNEL_REVIEW_REMINDERS,
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to review at your best study time"
                enableLights(true)
            }
            notificationManager.createNotificationChannel(reviewRemindersChannel)
        }
    }
}

