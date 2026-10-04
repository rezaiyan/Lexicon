package notification

import domain.notifications.usecase.ReportNotificationOpenedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import presentation.navigation.NotificationDestination
import presentation.navigation.NotificationNavigator

/**
 * Platform tap entry points (Android intent extras, iOS didReceive) hand the push data here:
 * the open is reported to the backend, and pushes about something actionable open its screen.
 * Reporting is fire-and-forget: a lost report only means one notification counts as ignored.
 */
class NotificationTapHandler(
    private val reportNotificationOpened: ReportNotificationOpenedUseCase,
    private val navigator: NotificationNavigator,
    private val scope: CoroutineScope,
) {
    fun onNotificationTapped(data: Map<String, String>) {
        destinationFor(data[TYPE_KEY])?.let(navigator::open)
        scope.launch { reportNotificationOpened(data) }
    }

    private fun destinationFor(type: String?): NotificationDestination? = when (type) {
        PushTypes.BILLING_ISSUE -> NotificationDestination.Subscription
        else -> null
    }

    companion object {
        const val TYPE_KEY = "type"
    }
}

/** Push `type` values sent by the backend that the app reacts to. */
object PushTypes {
    /** Visible: a renewal payment failed. Tapping opens the subscription screen. */
    const val BILLING_ISSUE = "billing_issue"

    /** Silent (data-only): the user's subscription changed on the server; refetch it. */
    const val SUBSCRIPTION_UPDATED = "subscription_updated"
}
