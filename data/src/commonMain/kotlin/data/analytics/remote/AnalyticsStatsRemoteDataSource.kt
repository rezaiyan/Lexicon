package data.analytics.remote

import core.common.Try
import core.common.doOnFailure
import core.common.doOnSuccess
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.HeatmapDayResponse
import data.analytics.remote.model.MonthlyStatsResponse
import data.analytics.remote.model.ResponseTimeTrendRemoteResponse
import data.analytics.remote.model.StudyInsightsResponse
import data.analytics.remote.model.StudySessionResponse
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncAnalyticsResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse
import data.core.network.client.ApiClient
import expects.logNetwork

class AnalyticsStatsRemoteDataSource(
    private val apiClient: ApiClient,
) : IAnalyticsStatsDataSource {

    override suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse> =
        apiClient.postNotNull<SyncAnalyticsResponse>("/analytics/sync", body = request)
            .doOnSuccess { logNetwork("AnalyticsRemote", "Synced ${it.syncedSessionIds.size} sessions") }
            .doOnFailure { logNetwork("AnalyticsRemote", "Sync failed: ${it.message}") }

    override suspend fun getInsights(): Try<StudyInsightsResponse> =
        apiClient.getNotNull("/analytics/insights")

    override suspend fun getRecentSessions(limit: Int): Try<List<StudySessionResponse>> =
        apiClient.getNotNull("/analytics/sessions?limit=$limit")

    override suspend fun getHeatmap(startMs: Long, endMs: Long): Try<List<HeatmapDayResponse>> =
        apiClient.getNotNull("/analytics/heatmap?start=$startMs&end=$endMs")

    override suspend fun getMonthlyStats(): Try<List<MonthlyStatsResponse>> =
        apiClient.getNotNull("/analytics/monthly-stats")

    override suspend fun getDailyStats(start: String, end: String): Try<List<DailyStatsRemoteResponse>> =
        apiClient.getNotNull("/analytics/daily-stats?start=$start&end=$end")

    override suspend fun getWeeklyReport(): Try<WeeklyReportRemoteResponse> =
        apiClient.getNotNull("/analytics/weekly-report")

    override suspend fun getResponseTimeTrend(): Try<List<ResponseTimeTrendRemoteResponse>> =
        apiClient.getNotNull("/analytics/response-time-trend")
}
