package data.insights.remote

import core.common.Try
import data.core.network.client.ApiClient
import io.ktor.client.request.parameter

interface IInsightsScreenRemoteDataSource {
    suspend fun fetch(timeZoneId: String): Try<InsightsScreenDto>
}

class InsightsScreenRemoteDataSource(
    private val apiClient: ApiClient,
) : IInsightsScreenRemoteDataSource {
    override suspend fun fetch(timeZoneId: String): Try<InsightsScreenDto> =
        apiClient.getNotNull<InsightsScreenDto>("/analytics/insights-screen") { parameter("tz", timeZoneId) }
}
