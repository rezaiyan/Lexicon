package data.connectivity

import core.common.Try
import core.common.getOrNull
import data.connectivity.remote.IServerHealthRemoteDataSource
import domain.connectivity.model.ServerStatus
import fakes.FakeNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServerStatusRepositoryImplTest {

    private class FakeHealth : IServerHealthRemoteDataSource {
        var up = true
        var pings = 0
            private set

        override suspend fun ping(): Try<Unit> {
            pings++
            return if (up) Try.success(Unit) else Try.failure(Exception("503"))
        }
    }

    private val health = FakeHealth()
    private val network = FakeNetworkMonitor()
    private val reachability = ServerReachability()

    private fun repository(scope: CoroutineScope) = ServerStatusRepositoryImpl(
        remote = health,
        network = network,
        reachability = reachability,
        scope = scope,
        recheckInterval = 30.seconds,
        retryDelays = listOf(5.seconds, 10.seconds),
    )

    /** Starts watching like the Settings screen does; returns the latest status. */
    private fun TestScope.watch(repository: ServerStatusRepositoryImpl): () -> ServerStatus? {
        var latest: ServerStatus? = null
        backgroundScope.launch { repository.observeStatus().collect { latest = it } }
        runCurrent()
        return { latest }
    }

    @Test
    fun `observeStatus when the health check passes reports online`() = runTest {
        val status = watch(repository(backgroundScope))

        assertEquals(ServerStatus.Online, status())
        assertEquals(1, health.pings)
    }

    @Test
    fun `observeStatus when the server is down reports unavailable, then online once a retry passes`() = runTest {
        health.up = false
        val status = watch(repository(backgroundScope))
        assertEquals(ServerStatus.Unavailable, status())

        health.up = true
        advanceTimeBy(5.seconds + 1.seconds)

        assertEquals(ServerStatus.Online, status())
    }

    @Test
    fun `observeStatus while down backs off between retries`() = runTest {
        health.up = false
        watch(repository(backgroundScope))

        advanceTimeBy(5.seconds + 1.seconds) // first retry after 5s
        assertEquals(2, health.pings)
        advanceTimeBy(5.seconds) // the next one waits 10s, not 5s
        assertEquals(2, health.pings)
        advanceTimeBy(5.seconds)
        assertEquals(3, health.pings)
    }

    @Test
    fun `observeStatus while up rechecks on the slow interval`() = runTest {
        watch(repository(backgroundScope))

        advanceTimeBy(29.seconds)
        assertEquals(1, health.pings)
        advanceTimeBy(2.seconds)
        assertEquals(2, health.pings)
    }

    @Test
    fun `observeStatus without network reports offline and doesn't call the server`() = runTest {
        network.connected.value = false

        val status = watch(repository(backgroundScope))
        advanceTimeBy(60.seconds)

        assertEquals(ServerStatus.Offline, status())
        assertEquals(0, health.pings)
    }

    @Test
    fun `observeStatus when the network comes back checks the server right away`() = runTest {
        network.connected.value = false
        val status = watch(repository(backgroundScope))

        network.connected.value = true
        runCurrent()

        assertEquals(ServerStatus.Online, status())
        assertEquals(1, health.pings)
    }

    @Test
    fun `a failed request ends the wait early, so an outage shows without waiting for the next recheck`() = runTest {
        val status = watch(repository(backgroundScope))
        health.up = false

        reachability.onRequestFailed()
        runCurrent()

        assertEquals(ServerStatus.Unavailable, status())
        assertEquals(2, health.pings)
    }

    @Test
    fun `an answered request shows online before the next health check`() = runTest {
        health.up = false
        val status = watch(repository(backgroundScope))

        reachability.onServerAnswered()
        runCurrent()

        assertEquals(ServerStatus.Online, status())
    }

    @Test
    fun `nobody watching means no health checks`() = runTest {
        repository(backgroundScope)

        advanceTimeBy(120.seconds)

        assertEquals(0, health.pings)
    }

    @Test
    fun `check when offline answers offline without calling the server`() = runTest {
        network.connected.value = false

        val result = repository(backgroundScope).check()

        assertEquals(ServerStatus.Offline, result.getOrNull())
        assertEquals(0, health.pings)
    }

    @Test
    fun `check reports the server's answer and updates the live status`() = runTest {
        health.up = false
        val repository = repository(backgroundScope)
        val status = watch(repository)
        health.up = true

        val result = repository.check()
        runCurrent()

        assertEquals(ServerStatus.Online, result.getOrNull())
        assertEquals(ServerStatus.Online, status())
    }
}
