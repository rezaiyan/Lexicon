package notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Push/local notification `type` of a review reminder; tapping one starts a review of the due cards. */
internal const val TYPE_REVIEW_REMINDER = "review_reminder"

/**
 * Opens the app when a locally scheduled notification is tapped, carrying its [type] as the `type`
 * extra that MainActivity hands to the notification tap handler.
 */
internal fun Context.openAppPendingIntent(requestCode: Int, type: String?): PendingIntent? {
    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        type?.let { putExtra("type", it) }
    } ?: return null
    return PendingIntent.getActivity(
        this,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
