package fakes

import core.common.Try
import domain.connectivity.INetworkMonitor
import domain.connectivity.model.ServerStatus
import domain.connectivity.repository.IServerStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The device's network; flip [connected] to simulate losing or regaining it. */
class FakeNetworkMonitor(connected: Boolean = true) : INetworkMonitor {
    val connected = MutableStateFlow(connected)
    override val isConnected: Flow<Boolean> = this.connected
}

class FakeServerStatusRepository(status: ServerStatus = ServerStatus.Checking) : IServerStatusRepository {
    val status = MutableStateFlow(status)

    /** What the next [check] finds; it also becomes the live status, like a real check would. */
    var nextCheck: ServerStatus = ServerStatus.Online
    var checks = 0
        private set

    override fun observeStatus(): Flow<ServerStatus> = status

    override suspend fun check(): Try<ServerStatus> {
        checks++
        status.value = nextCheck
        return Try.success(nextCheck)
    }
}
