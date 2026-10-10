package presentation.navigation

import domain.word.model.ReviewSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Screens a tapped notification can open. */
enum class NotificationDestination { Subscription, Study, Insights, Words }

/**
 * Hands a notification tap from the platform layer to the app's navigation.
 *
 * A tap can arrive before the UI exists (cold start from the notification), so the request is
 * buffered until the app shell collects it; conflated, so only the latest tap counts.
 */
class NotificationNavigator {

    private val requests = Channel<NotificationDestination>(Channel.CONFLATED)
    private val reviews = Channel<ReviewSource>(Channel.CONFLATED)
    private val wordRush = Channel<Unit>(Channel.CONFLATED)
    private val addWords = Channel<Unit>(Channel.CONFLATED)

    /** One-shot destinations; collect with OnEvents. */
    val destinations: Flow<NotificationDestination> = requests.receiveAsFlow()

    /** One-shot requests to start a review; collected by the Study screen once it's shown. */
    val reviewRequests: Flow<ReviewSource> = reviews.receiveAsFlow()

    /** One-shot requests to start a Word Rush game; collected by the Study screen once it's shown. */
    val wordRushRequests: Flow<Unit> = wordRush.receiveAsFlow()

    /** One-shot requests to open the add-words sheet; collected by the Study screen once it's shown. */
    val addWordsRequests: Flow<Unit> = addWords.receiveAsFlow()

    fun open(destination: NotificationDestination) {
        requests.trySend(destination)
    }

    /** Switches to the Study tab and starts a review of the due cards there. */
    fun openDueReview() = openReview(ReviewSource.DueCards)

    /** Switches to the Study tab and starts a review of [source] there. */
    fun openReview(source: ReviewSource) {
        open(NotificationDestination.Study)
        reviews.trySend(source)
    }

    /** Switches to the Study tab and starts a Word Rush game there. */
    fun openWordRush() {
        open(NotificationDestination.Study)
        wordRush.trySend(Unit)
    }

    /** Switches to the Study tab and opens the add-words sheet there. */
    fun openAddWords() {
        open(NotificationDestination.Study)
        addWords.trySend(Unit)
    }
}
