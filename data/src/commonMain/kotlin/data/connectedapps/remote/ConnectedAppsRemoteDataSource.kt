package data.connectedapps.remote

import core.common.Try
import core.common.doOnFailure
import data.core.network.client.ApiClient
import expects.logNetwork
import kotlinx.serialization.Serializable

@Serializable
data class ConnectedAppResponse(
    val id: String,
    val clientName: String,
    val scopes: List<String> = emptyList(),
    /** Epoch millis. */
    val connectedAt: Long,
)

interface IConnectedAppsRemoteDataSource {
    suspend fun getConnections(): Try<List<ConnectedAppResponse>>
    suspend fun disconnect(id: String): Try<Unit>
}

class ConnectedAppsRemoteDataSource(
    private val apiClient: ApiClient,
) : IConnectedAppsRemoteDataSource {

    override suspend fun getConnections(): Try<List<ConnectedAppResponse>> =
        apiClient.getNotNull<List<ConnectedAppResponse>>(PATH)
            .doOnFailure { logNetwork("ConnectedAppsRemote", "Fetch failed: ${it.message}") }

    override suspend fun disconnect(id: String): Try<Unit> =
        apiClient.delete("$PATH/$id")
            .doOnFailure { logNetwork("ConnectedAppsRemote", "Disconnect failed: ${it.message}") }

    private companion object {
        const val PATH = "/mcp/connections"
    }
}
