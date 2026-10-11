package notification.payload

import domain.support.usecase.SyncSupportRepliesUseCase
import notification.PushTypes

/**
 * Support answered while the app is running: pull the reply into the local conversation so the
 * unread dots on Settings and Help & Support show it at once.
 */
class SupportReplyHandler(
    private val syncSupportReplies: SyncSupportRepliesUseCase,
) : NotificationPayloadHandler {

    override val type: String = PushTypes.SUPPORT_REPLY

    override suspend fun handle(data: Map<String, String>) {
        syncSupportReplies(Unit)
    }
}
