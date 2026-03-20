package data.analytics.remote

import core.common.Try
import core.common.doOnFailure
import core.common.doOnSuccess
import data.analytics.remote.model.AccuracyByLevelResponse
import data.analytics.remote.model.ComebackWordResponse
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.DayOfWeekAccuracyResponse
import data.analytics.remote.model.DifficultWordResponse
import data.analytics.remote.model.HeatmapDayResponse
import data.analytics.remote.model.HourlyAccuracyResponse
import data.analytics.remote.model.LanguagePairStatsResponse
import data.analytics.remote.model.MasteredWordResponse
import data.analytics.remote.model.MonthlyStatsResponse
import data.analytics.remote.model.MostReviewedWordResponse
import data.analytics.remote.model.StudyInsightsResponse
import data.analytics.remote.model.StudySessionResponse
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncAnalyticsResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse
import data.core.network.client.ApiClient
import expects.logNetwork

class AnalyticsRemoteDataSource(
    private val apiClient: ApiClient,
) : IAnalyticsStatsDataSource, IAnalyticsWordDataSource {

    override suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse> =
        apiClient.postNotNull<SyncAnalyticsResponse>("/analytics/sync", body = request)
            .doOnSuccess { logNetwork("AnalyticsRemote", "Synced ${it.syncedSessionIds.size} sessions") }
            .doOnFailure { logNetwork("AnalyticsRemote", "Sync failed: ${it.message}") }

    override suspend fun getInsights(): Try<StudyInsightsResponse> =
        apiClient.getNotNull("/analytics/insights")

    override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<DifficultWordResponse>> =
        apiClient.getNotNull("/analytics/difficult-words?minReviews=$minReviews&limit=$limit")

    override suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWordResponse>> =
        apiClient.getNotNull("/analytics/most-reviewed?limit=$limit")

    override suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevelResponse>> =
        apiClient.getNotNull("/analytics/accuracy-by-level")

    override suspend fun getAccuracyByHour(): Try<List<HourlyAccuracyResponse>> =
        apiClient.getNotNull("/analytics/accuracy-by-hour")

    override suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracyResponse>> =
        apiClient.getNotNull("/analytics/accuracy-by-day-of-week")

    override suspend fun getRecentSessions(limit: Int): Try<List<StudySessionResponse>> =
        apiClient.getNotNull("/analytics/sessions?limit=$limit")

    override suspend fun getHeatmap(startMs: Long, endMs: Long): Try<List<HeatmapDayResponse>> =
        apiClient.getNotNull("/analytics/heatmap?start=$startMs&end=$endMs")

    override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWordResponse>> =
        apiClient.getNotNull("/analytics/words-mastered?limit=$limit")

    override suspend fun getLanguageStats(): Try<List<LanguagePairStatsResponse>> =
        apiClient.getNotNull("/analytics/language-stats")

    override suspend fun getMonthlyStats(): Try<List<MonthlyStatsResponse>> =
        apiClient.getNotNull("/analytics/monthly-stats")

    override suspend fun getComebackWords(): Try<List<ComebackWordResponse>> =
        apiClient.getNotNull("/analytics/comeback-words")

    override suspend fun getDailyStats(start: String, end: String): Try<List<DailyStatsRemoteResponse>> =
        apiClient.getNotNull("/analytics/daily-stats?start=$start&end=$end")

    override suspend fun getWeeklyReport(): Try<WeeklyReportRemoteResponse> =
        apiClient.getNotNull("/analytics/weekly-report")
}
