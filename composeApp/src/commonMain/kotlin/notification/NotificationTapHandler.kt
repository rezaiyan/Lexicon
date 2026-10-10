package notification

import domain.notifications.usecase.ReportNotificationOpenedUseCase
import domain.word.model.ReviewSource
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
        when (data[TYPE_KEY]) {
            PushTypes.BILLING_ISSUE -> navigator.open(NotificationDestination.Subscription)
            PushTypes.REVIEW_REMINDER -> navigator.openDueReview()
            else -> data[DEEP_LINK_KEY]?.let(::openDeepLink)
        }
        scope.launch { reportNotificationOpened(data) }
    }

    /**
     * Smart pushes (due cards, streak, insight, milestone...) say where they lead with a
     * `deep_link`. Only these known links are followed; anything else just opens the app.
     */
    private fun openDeepLink(link: String) {
        when {
            link == "vokab://review" || link.startsWith("vokab://review/") -> navigator.openDueReview()
            link == "vokab://words/add" -> navigator.openAddWords()
            link.startsWith(WORD_LINK_PREFIX) -> openWord(link.removePrefix(WORD_LINK_PREFIX))
            link == "vokab://insights" || link.startsWith("vokab://stats/") ->
                navigator.open(NotificationDestination.Insights)
        }
    }

    /** "<word> wants a rematch": review just that word; a malformed id falls back to the Words tab. */
    private fun openWord(id: String) {
        val wordId = id.toLongOrNull()
        if (wordId != null) {
            navigator.openReview(ReviewSource.ByWords(listOf(wordId)))
        } else {
            navigator.open(NotificationDestination.Words)
        }
    }

    companion object {
        const val TYPE_KEY = "type"
        const val DEEP_LINK_KEY = "deep_link"
        private const val WORD_LINK_PREFIX = "vokab://word/"
    }
}

/** Push `type` values sent by the backend that the app reacts to. */
object PushTypes {
    /** Visible: a renewal payment failed. Tapping opens the subscription screen. */
    const val BILLING_ISSUE = "billing_issue"

    /** Visible: cards are due. Tapping starts a review of the due cards. */
    const val REVIEW_REMINDER = "review_reminder"

    /** Silent (data-only): the user's subscription changed on the server; refetch it. */
    const val SUBSCRIPTION_UPDATED = "subscription_updated"
}
