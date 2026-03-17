package data.analytics.remote

import core.common.Try
import data.analytics.remote.model.*

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
