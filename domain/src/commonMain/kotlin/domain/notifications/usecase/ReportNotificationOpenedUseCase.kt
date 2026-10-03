package domain.notifications.usecase

import core.common.Try
import core.common.UseCase
import domain.notifications.repository.INotificationEngagementRepository

/**
 * Reports a notification tap. [params] is the push data payload; only server-sent
 * smart notifications carry a log id — anything else (local reminders) is a no-op.
 */
class ReportNotificationOpenedUseCase(
    private val repository: INotificationEngagementRepository
) : UseCase<Map<String, String>, Unit> {

    override suspend fun invoke(params: Map<String, String>): Try<Unit> {
        val logId = params[NOTIFICATION_LOG_ID_KEY]?.toLongOrNull() ?: return Try.success(Unit)
        return repository.reportOpened(logId)
    }

    companion object {
        const val NOTIFICATION_LOG_ID_KEY = "notification_log_id"
    }
}
