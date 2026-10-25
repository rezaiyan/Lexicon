package data.connectivity.remote

import core.common.Try
import core.common.map
import data.core.network.client.ApiClient
import data.core.network.interceptor.SkipReachabilityReport
import io.ktor.client.plugins.timeout
import kotlinx.serialization.json.JsonObject

interface IServerHealthRemoteDataSource {
    /** Success when the server is up and ready (it answers 503 while its database is down). */
    suspend fun ping(): Try<Unit>
}

class ServerHealthRemoteDataSource(
    private val apiClient: ApiClient,
) : IServerHealthRemoteDataSource {

    override suspend fun ping(): Try<Unit> =
        apiClient.get<JsonObject>(PATH) {
            attributes.put(SkipReachabilityReport, Unit)
            // The client's default waits two minutes for AI calls; a status check must answer fast
            timeout { requestTimeoutMillis = TIMEOUT_MILLIS }
        }.map { }

    private companion object {
        const val PATH = "/health"
        const val TIMEOUT_MILLIS = 10_000L
    }
}
