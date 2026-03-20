package domain.analytics.repository

import core.common.Try
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.MonthlyStats
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.StudySession
import domain.analytics.model.WeeklyReport

interface IAnalyticsStatsRepository {
    suspend fun getStudyInsights(): Try<StudyInsights>
    suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>>
    suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>>
    suspend fun getRecentSessions(limit: Int): Try<List<StudySession>>
    suspend fun getWeeklyReport(): Try<WeeklyReport>
    suspend fun getMonthlyStats(): Try<List<MonthlyStats>>
    suspend fun syncToBackend(): Try<Int>
}
