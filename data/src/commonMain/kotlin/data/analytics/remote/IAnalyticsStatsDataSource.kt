package data.analytics.remote

import core.common.Try
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.HeatmapDayResponse
import data.analytics.remote.model.MonthlyStatsResponse
import data.analytics.remote.model.StudyInsightsResponse
import data.analytics.remote.model.StudySessionResponse
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncAnalyticsResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse

interface IAnalyticsStatsDataSource {
    suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse>
    suspend fun getInsights(): Try<StudyInsightsResponse>
    suspend fun getDailyStats(start: String, end: String): Try<List<DailyStatsRemoteResponse>>
    suspend fun getHeatmap(startMs: Long, endMs: Long): Try<List<HeatmapDayResponse>>
    suspend fun getRecentSessions(limit: Int): Try<List<StudySessionResponse>>
    suspend fun getWeeklyReport(): Try<WeeklyReportRemoteResponse>
    suspend fun getMonthlyStats(): Try<List<MonthlyStatsResponse>>
}
