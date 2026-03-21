package data.analytics.repository

import core.common.Try
import core.common.map
import data.analytics.mapper.toDomain
import data.analytics.remote.IAnalyticsStatsDataSource
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.MonthlyStats
import domain.analytics.model.ResponseTimeTrend
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.StudySession
import domain.analytics.model.WeeklyReport
import domain.analytics.repository.IAnalyticsStatsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private fun dateToEpochMs(dateStr: String, tz: TimeZone): Long {
    val date = kotlinx.datetime.LocalDate.parse(dateStr)
    val dateTime = kotlinx.datetime.LocalDateTime(date.year, date.month, date.day, 0, 0, 0)
    return dateTime.toInstant(tz).toEpochMilliseconds()
}

class AnalyticsStatsRepositoryImpl(
    private val statsDataSource: IAnalyticsStatsDataSource,
) : IAnalyticsStatsRepository {

    override suspend fun getStudyInsights(): Try<StudyInsights> =
        statsDataSource.getInsights().map { response ->
            StudyInsights(
                totalCardsReviewed = response.totalCardsReviewed,
                totalCorrect = response.totalCorrect,
                accuracyPercent = response.accuracyPercent,
                totalStudyTimeMs = response.totalStudyTimeMs,
                totalSessions = response.totalSessions,
                daysStudied = response.daysStudied,
                uniqueWordsReviewed = response.uniqueWordsReviewed,
                averageResponseTimeMs = response.averageResponseTimeMs,
                averageSessionDurationMs = response.averageSessionDurationMs,
                sessionCompletionRate = response.sessionCompletionRate,
                wordsMasteredCount = response.wordsMasteredCount,
            )
        }

    override suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>> =
        statsDataSource.getDailyStats(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>> {
        val tz = TimeZone.currentSystemDefault()
        val startMs = dateToEpochMs(startDate, tz)
        val endMs = dateToEpochMs(endDate, tz) + 86_400_000L - 1
        return statsDataSource.getHeatmap(startMs, endMs).map { list ->
            list.map { r -> StudyHeatmapDay(date = r.date, count = r.count) }
        }
    }

    override suspend fun getRecentSessions(limit: Int): Try<List<StudySession>> =
        statsDataSource.getRecentSessions(limit).map { list ->
            list.map { r ->
                StudySession(
                    sessionId = r.clientSessionId,
                    startedAt = r.startedAt,
                    endedAt = r.endedAt,
                    durationMs = r.durationMs,
                    totalCards = r.totalCards,
                    correctCount = r.correctCount,
                    incorrectCount = r.incorrectCount,
                    reviewType = r.reviewType,
                    completedNormally = r.completedNormally,
                )
            }
        }

    override suspend fun getWeeklyReport(): Try<WeeklyReport> =
        statsDataSource.getWeeklyReport().map { it.toDomain() }

    override suspend fun getMonthlyStats(): Try<List<MonthlyStats>> =
        statsDataSource.getMonthlyStats().map { list ->
            list.map { r ->
                MonthlyStats(
                    year = r.year,
                    month = r.month,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun syncToBackend(): Try<Int> =
        Try.success(0) // No local data to sync — everything goes directly to backend

    override suspend fun getResponseTimeTrend(): Try<List<ResponseTimeTrend>> =
        statsDataSource.getResponseTimeTrend().map { list ->
            list.map { r ->
                ResponseTimeTrend(
                    year = r.year,
                    week = r.week,
                    avgResponseTimeMs = r.avgResponseTimeMs,
                )
            }
        }
}
