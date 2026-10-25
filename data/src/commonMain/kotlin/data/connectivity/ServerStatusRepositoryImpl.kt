package data.connectivity

import core.common.Try
import data.connectivity.remote.IServerHealthRemoteDataSource
import domain.connectivity.INetworkMonitor
import domain.connectivity.model.ServerStatus
import domain.connectivity.repository.IServerStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Server status from three sources: the OS network state, ordinary requests (via [ServerReachability])
 * and the health check. Health checks only run while someone watches the status, so a closed screen
 * costs nothing: every [recheckInterval] while up, and sooner (backing off by [retryDelays]) while down.
 *
 * @param scope outlives screens; holds the shared status so all watchers see one check loop.
 */
class ServerStatusRepositoryImpl(
    private val remote: IServerHealthRemoteDataSource,
    private val network: INetworkMonitor,
    private val reachability: ServerReachability,
    scope: CoroutineScope,
    private val recheckInterval: Duration = 30.seconds,
    private val retryDelays: List<Duration> = listOf(5.seconds, 10.seconds, 20.seconds, 30.seconds),
) : IServerStatusRepository {

    // One health check at a time: a retry tap while the loop is checking waits for that answer
    private val checks = Mutex()

    private val status: Flow<ServerStatus> = channelFlow {
        launch { keepChecking() }
        combine(network.isConnected, reachability.reachability, ::statusOf).collect { send(it) }
    }
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    override fun observeStatus(): Flow<ServerStatus> = status

    override suspend fun check(): Try<ServerStatus> {
        if (!network.isConnected.first()) return Try.success(ServerStatus.Offline)
        return Try.success(if (ping()) ServerStatus.Online else ServerStatus.Unavailable)
    }

    private suspend fun keepChecking() {
        network.isConnected.distinctUntilChanged().collectLatest { connected ->
            if (!connected) {
                reachability.forget()
                return@collectLatest
            }
            var failures = 0
            while (true) {
                val reachable = ping()
                failures = if (reachable) 0 else failures + 1
                val wait = if (reachable) recheckInterval else retryDelays[minOf(failures, retryDelays.size) - 1]
                // A failed request elsewhere ends the wait early: it may be the first sign of an outage
                withTimeoutOrNull(wait) { reachability.checkRequests.first() }
            }
        }
    }

    private suspend fun ping(): Boolean = checks.withLock {
        remote.ping().isSuccess.also(reachability::record)
    }

    private fun statusOf(connected: Boolean, reachability: Reachability): ServerStatus = when {
        !connected -> ServerStatus.Offline
        reachability == Reachability.Reachable -> ServerStatus.Online
        reachability == Reachability.Unreachable -> ServerStatus.Unavailable
        else -> ServerStatus.Checking
    }

    private companion object {
        // Survives a quick screen change or rotation without restarting the check loop
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
