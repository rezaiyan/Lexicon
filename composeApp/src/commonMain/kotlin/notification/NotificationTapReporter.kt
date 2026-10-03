package notification

import domain.notifications.usecase.ReportNotificationOpenedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Platform tap entry points (Android intent extras, iOS didReceive) hand the push data here.
 * Fire-and-forget: a lost report only means the backend counts this one notification as ignored.
 */
class NotificationTapReporter(
    private val reportNotificationOpened: ReportNotificationOpenedUseCase,
    private val scope: CoroutineScope,
) {
    fun onNotificationTapped(data: Map<String, String>) {
        scope.launch { reportNotificationOpened(data) }
    }
}
