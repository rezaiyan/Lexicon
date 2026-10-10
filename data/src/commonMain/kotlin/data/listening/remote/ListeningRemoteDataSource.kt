package data.listening.remote

import core.common.Try
import core.common.doOnFailure
import core.common.doOnSuccess
import data.core.network.client.ApiClient
import expects.logNetwork

interface IListeningDataSource {
    suspend fun syncSessions(sessions: List<SyncListeningSessionRequest>): Try<Unit>
}

class ListeningRemoteDataSource(
    private val apiClient: ApiClient,
) : IListeningDataSource {

    override suspend fun syncSessions(sessions: List<SyncListeningSessionRequest>): Try<Unit> =
        apiClient.postUnit("/listening/sync", body = SyncListeningRequest(sessions = sessions))
            .doOnSuccess { logNetwork("ListeningRemote", "Synced ${sessions.size} sessions") }
            .doOnFailure { logNetwork("ListeningRemote", "Sync failed: ${it.message}") }
}
