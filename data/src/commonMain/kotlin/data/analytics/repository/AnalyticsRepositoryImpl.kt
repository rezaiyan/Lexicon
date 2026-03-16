package data.analytics.repository

import core.common.Try
import core.common.map
import data.analytics.remote.IAnalyticsRemoteDataSource
import domain.analytics.model.*
import domain.analytics.repository.IAnalyticsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Analytics repository that reads all data from the backend.
 * No local storage — all queries go to the server.
 */
class AnalyticsRepositoryImpl(
    private val remoteDataSource: IAnalyticsRemoteDataSource,
) : IAnalyticsRepository {

    override suspend fun getStudyInsights(): Try<StudyInsights> =
        remoteDataSource.getInsights().map { r ->
            StudyInsights(
                totalCardsReviewed = r.totalCardsReviewed,
                totalCorrect = r.totalCorrect,
                accuracyPercent = r.accuracyPercent,
                totalStudyTimeMs = r.totalStudyTimeMs,
                totalSessions = r.totalSessions,
                daysStudied = r.daysStudied,
                uniqueWordsReviewed = r.uniqueWordsReviewed,
                averageResponseTimeMs = r.averageResponseTimeMs,
                averageSessionDurationMs = r.averageSessionDurationMs,
                wordsMasteredCount = r.wordsMasteredCount,
            )
        }

    override suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>> {
        // Backend daily-stats endpoint expects date strings
        // For now, derive from heatmap + sessions if needed, or use insights
        // The backend has /daily-stats?start=&end= but returns DailyStatsResponse
        return Try.success(emptyList())
    }

    override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> =
        remoteDataSource.getDifficultWords(minReviews, limit).map { list ->
            list.map { r ->
                WordDifficulty(
                    wordId = r.wordId.toInt(),
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                    sourceLanguage = r.sourceLanguage,
                    targetLanguage = r.targetLanguage,
                    totalReviews = r.totalReviews,
                    errorCount = r.errorCount,
                    errorRate = r.errorRate,
                )
            }
        }

    override suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>> =
        remoteDataSource.getMostReviewedWords(limit).map { list ->
            list.map { r ->
                MostReviewedWord(
                    wordId = r.wordId.toInt(),
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                    totalReviews = r.totalReviews,
                )
            }
        }

    override suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>> =
        remoteDataSource.getAccuracyByLevel().map { list ->
            list.map { r ->
                AccuracyByLevel(
                    level = r.level,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>> =
        remoteDataSource.getAccuracyByHour().map { list ->
            list.map { r ->
                HourlyAccuracy(
                    hour = r.hour,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>> =
        remoteDataSource.getAccuracyByDayOfWeek().map { list ->
            list.map { r ->
                DayOfWeekAccuracy(
                    dayOfWeek = r.dayOfWeek,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun getRecentSessions(limit: Int): Try<List<StudySession>> =
        remoteDataSource.getRecentSessions(limit).map { list ->
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

    override suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>> {
        val tz = TimeZone.currentSystemDefault()
        val startMs = dateToEpochMs(startDate, tz)
        val endMs = dateToEpochMs(endDate, tz) + 86_400_000L - 1
        return remoteDataSource.getHeatmap(startMs, endMs).map { list ->
            list.map { r -> StudyHeatmapDay(date = r.date, count = r.count) }
        }
    }

    private fun dateToEpochMs(dateStr: String, tz: TimeZone): Long {
        val date = kotlinx.datetime.LocalDate.parse(dateStr)
        val dateTime = kotlinx.datetime.LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 0, 0, 0)
        return dateTime.toInstant(tz).toEpochMilliseconds()
    }

    override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>> =
        remoteDataSource.getWordsMastered(limit).map { list ->
            list.map { r ->
                MasteredWord(
                    wordId = r.wordId.toInt(),
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                    masteredAt = r.masteredAt,
                )
            }
        }

    override suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>> =
        remoteDataSource.getLanguageStats().map { list ->
            list.map { r ->
                LanguagePairStats(
                    sourceLanguage = r.sourceLanguage,
                    targetLanguage = r.targetLanguage,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    uniqueWords = r.uniqueWords,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun getMonthlyStats(): Try<List<MonthlyStats>> =
        remoteDataSource.getMonthlyStats().map { list ->
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

    override suspend fun getComebackWords(): Try<List<ComebackWord>> =
        remoteDataSource.getComebackWords().map { list ->
            list.map { r ->
                ComebackWord(
                    wordId = r.wordId.toInt(),
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                )
            }
        }

    override suspend fun syncToBackend(): Try<Int> =
        Try.success(0) // No local data to sync — everything goes directly to backend
}
