package data.analytics.repository

import core.common.Try
import core.common.map
import data.analytics.mapper.toDomain
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.IAnalyticsWordDataSource
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.ComebackWord
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.LanguagePairStats
import domain.analytics.model.MasteredWord
import domain.analytics.model.MonthlyStats
import domain.analytics.model.MostReviewedWord
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.StudySession
import domain.analytics.model.WeeklyReport
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsStatsRepository
import domain.analytics.repository.IAnalyticsWordRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private fun dateToEpochMs(dateStr: String, tz: TimeZone): Long {
    val date = kotlinx.datetime.LocalDate.parse(dateStr)
    val dateTime = kotlinx.datetime.LocalDateTime(date.year, date.month, date.day, 0, 0, 0)
    return dateTime.toInstant(tz).toEpochMilliseconds()
}

/**
 * Analytics repository that reads all data from the backend.
 * No local storage — all queries go to the server.
 */
class AnalyticsRepositoryImpl(
    private val statsDataSource: IAnalyticsStatsDataSource,
    private val wordDataSource: IAnalyticsWordDataSource,
) : IAnalyticsStatsRepository, IAnalyticsWordRepository {

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

    override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> =
        wordDataSource.getDifficultWords(minReviews, limit).map { list ->
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
        wordDataSource.getMostReviewedWords(limit).map { list ->
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
        wordDataSource.getAccuracyByLevel().map { list ->
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
        wordDataSource.getAccuracyByHour().map { list ->
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
        wordDataSource.getAccuracyByDayOfWeek().map { list ->
            list.map { r ->
                DayOfWeekAccuracy(
                    dayOfWeek = r.dayOfWeek,
                    totalReviews = r.totalReviews,
                    correctCount = r.correctCount,
                    accuracyPercent = r.accuracyPercent,
                )
            }
        }

    override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>> =
        wordDataSource.getWordsMastered(limit).map { list ->
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
        wordDataSource.getLanguageStats().map { list ->
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

    override suspend fun getComebackWords(): Try<List<ComebackWord>> =
        wordDataSource.getComebackWords().map { list ->
            list.map { r ->
                ComebackWord(
                    wordId = r.wordId.toInt(),
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                )
            }
        }
}
