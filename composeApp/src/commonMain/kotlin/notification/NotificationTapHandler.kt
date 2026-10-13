package notification

import domain.notifications.usecase.ReportNotificationOpenedUseCase
import domain.word.model.ReviewSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import media.LISTENING_NOTIFICATION_TYPE
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
        // The listening player's own media notification: local, nothing to report.
        if (data[TYPE_KEY] == LISTENING_NOTIFICATION_TYPE) {
            navigator.openListening()
            return
        }
        when (data[TYPE_KEY]) {
            PushTypes.BILLING_ISSUE -> navigator.open(NotificationDestination.Subscription)
            // Newer reminders offer a few due words; older ones carry no link of their own
            PushTypes.REVIEW_REMINDER ->
                data[DEEP_LINK_KEY]?.let { openDeepLink(it, data) } ?: navigator.openDueReview()
            PushTypes.SUPPORT_REPLY -> navigator.open(NotificationDestination.Support)
            else -> data[DEEP_LINK_KEY]?.let { openDeepLink(it, data) }
        }
        scope.launch { reportNotificationOpened(data) }
    }

    /**
     * Smart pushes (due cards, streak, insight, milestone...) say where they lead with a
     * `deep_link`. Only these known links are followed; anything else just opens the app.
     */
    private fun openDeepLink(link: String, data: Map<String, String>) {
        when {
            // Before the generic review link, which older app versions follow for this push too
            link.startsWith(RESCUE_LINK_PREFIX) -> openRescue(link, data)
            link.startsWith(BITE_LINK_PREFIX) -> openBite(link)
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

    /**
     * A due push's bite: review just the few due words it offered rather than the whole backlog.
     * Anything unusable falls back to the due review.
     */
    private fun openBite(link: String) {
        val wordIds = wordIds(queryOf(link))
        if (wordIds.isEmpty()) navigator.openDueReview() else navigator.openReview(ReviewSource.ByWords(wordIds))
    }

    /**
     * A comeback rescue: review exactly the words planned for this learner, in their order, with
     * the outros from the push. Anything unusable falls back to the due review.
     */
    private fun openRescue(link: String, data: Map<String, String>) {
        val query = queryOf(link)
        val rescueId = (query[RESCUE_ID_KEY] ?: data[RESCUE_ID_KEY])?.toLongOrNull()
        val wordIds = wordIds(query)
        if (rescueId == null || wordIds.isEmpty()) {
            navigator.openDueReview()
            return
        }
        navigator.openReview(
            ReviewSource.ComebackRescue(rescueId, wordIds, data[OUTRO_WIN_KEY], data[OUTRO_SOFT_KEY])
        )
    }

    private fun queryOf(link: String): Map<String, String> =
        link.substringAfter('?', "").split('&')
            .mapNotNull { param -> param.split('=', limit = 2).takeIf { it.size == 2 } }
            .associate { (key, value) -> key to value }

    private fun wordIds(query: Map<String, String>): List<Long> =
        query["ids"].orEmpty().split(',')
            .mapNotNull { it.trim().toLongOrNull()?.takeIf { id -> id > 0 } }
            .take(MAX_LINK_WORDS)

    companion object {
        const val TYPE_KEY = "type"
        const val DEEP_LINK_KEY = "deep_link"
        private const val WORD_LINK_PREFIX = "vokab://word/"
        private const val RESCUE_LINK_PREFIX = "vokab://review/rescue"
        private const val BITE_LINK_PREFIX = "vokab://review/words"
        private const val RESCUE_ID_KEY = "rid"
        private const val OUTRO_WIN_KEY = "outro_win"
        private const val OUTRO_SOFT_KEY = "outro_soft"
        /** Rescues and due bites offer about 8 words; anything far beyond that is not one. */
        private const val MAX_LINK_WORDS = 20
    }
}

/** Push `type` values sent by the backend that the app reacts to. */
object PushTypes {
    /** Visible: a renewal payment failed. Tapping opens the subscription screen. */
    const val BILLING_ISSUE = "billing_issue"

    /** Visible: cards are due. Tapping starts a review of the due cards. */
    const val REVIEW_REMINDER = "review_reminder"

    /** Visible: support answered the user's message. Tapping opens the support chat. */
    const val SUPPORT_REPLY = "support_reply"

    /** Silent (data-only): the user's subscription changed on the server; refetch it. */
    const val SUBSCRIPTION_UPDATED = "subscription_updated"
}
