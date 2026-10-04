package presentation.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Screens a tapped notification can open. */
enum class NotificationDestination { Subscription }

/**
 * Hands a notification tap from the platform layer to the app's navigation.
 *
 * A tap can arrive before the UI exists (cold start from the notification), so the request is
 * buffered until the app shell collects it; conflated, so only the latest tap counts.
 */
class NotificationNavigator {

    private val requests = Channel<NotificationDestination>(Channel.CONFLATED)

    /** One-shot destinations; collect with OnEvents. */
    val destinations: Flow<NotificationDestination> = requests.receiveAsFlow()

    fun open(destination: NotificationDestination) {
        requests.trySend(destination)
    }
}
