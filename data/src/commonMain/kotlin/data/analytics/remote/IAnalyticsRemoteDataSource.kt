package data.analytics.remote

import core.common.Try
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

interface IAnalyticsRemoteDataSource {
    suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse>
    suspend fun getInsights(): Try<StudyInsightsResponse>
    suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<DifficultWordResponse>>
    suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWordResponse>>
    suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevelResponse>>
    suspend fun getAccuracyByHour(): Try<List<HourlyAccuracyResponse>>
    suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracyResponse>>
    suspend fun getRecentSessions(limit: Int): Try<List<StudySessionResponse>>
    suspend fun getHeatmap(startMs: Long, endMs: Long): Try<List<HeatmapDayResponse>>
    suspend fun getWordsMastered(limit: Int): Try<List<MasteredWordResponse>>
    suspend fun getLanguageStats(): Try<List<LanguagePairStatsResponse>>
    suspend fun getMonthlyStats(): Try<List<MonthlyStatsResponse>>
    suspend fun getComebackWords(): Try<List<ComebackWordResponse>>
    suspend fun getDailyStats(start: String, end: String): Try<List<DailyStatsRemoteResponse>>
    suspend fun getWeeklyReport(): Try<WeeklyReportRemoteResponse>
}
