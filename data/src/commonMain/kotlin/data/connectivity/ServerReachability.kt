package data.connectivity

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

enum class Reachability { Unknown, Reachable, Unreachable }

/**
 * What the app last learned about reaching the server, from the health check and from every
 * ordinary request. Has no dependencies, so the HTTP client can feed it without a DI cycle.
 */
class ServerReachability {

    private val state = MutableStateFlow(Reachability.Unknown)
    val reachability: StateFlow<Reachability> = state.asStateFlow()

    // Conflated: a burst of failed requests asks for one check, not one each
    private val checks = Channel<Unit>(Channel.CONFLATED)

    /** Asks for a health check soon, because a request just failed in a way that may mean the server is down. */
    val checkRequests: Flow<Unit> = checks.receiveAsFlow()

    /** Any answer that isn't a server error proves the server is up; no check needed. */
    fun onServerAnswered() {
        state.value = Reachability.Reachable
    }

    /**
     * A request couldn't get an answer, or got a 5xx. One slow AI call or one failing endpoint doesn't
     * mean the server is down, so this asks the health check to decide instead of flipping the state.
     */
    fun onRequestFailed() {
        checks.trySend(Unit)
    }

    /** The health check's verdict. */
    fun record(reachable: Boolean) {
        state.value = if (reachable) Reachability.Reachable else Reachability.Unreachable
    }

    /** No network: what was known about the server no longer says anything about the next attempt. */
    fun forget() {
        state.value = Reachability.Unknown
    }
}
