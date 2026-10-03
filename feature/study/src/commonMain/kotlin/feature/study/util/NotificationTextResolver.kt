package feature.study.util

import org.jetbrains.compose.resources.getString

data class NotificationText(val title: String, val message: String)

/** Resolves localized review-reminder text; injectable so ViewModels stay testable off-device. */
fun interface NotificationTextResolver {
    suspend fun resolve(dueCards: Int): NotificationText
}

object ComposeNotificationTextResolver : NotificationTextResolver {
    override suspend fun resolve(dueCards: Int): NotificationText {
        val strings = NotificationStringHelper.getNotificationResources(dueCards)
        return NotificationText(
            title = getString(strings.titleRes, *strings.titleParams.toTypedArray()),
            message = getString(strings.messageRes, *strings.messageParams.toTypedArray()),
        )
    }
}
