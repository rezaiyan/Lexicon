package data.analytics.repository

import core.common.Try
import core.common.map
import data.analytics.remote.IAnalyticsWordDataSource
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.ComebackWord
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.LanguagePairStats
import domain.analytics.model.LevelTransition
import domain.analytics.model.MasteredWord
import domain.analytics.model.MostReviewedWord
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsWordRepository

class AnalyticsWordRepositoryImpl(
    private val wordDataSource: IAnalyticsWordDataSource,
) : IAnalyticsWordRepository {

    override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> =
        wordDataSource.getDifficultWords(minReviews, limit).map { list ->
            list.map { r ->
                WordDifficulty(
                    wordId = r.wordId,
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
                    wordId = r.wordId,
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
                    wordId = r.wordId,
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
                    wordId = r.wordId,
                    wordText = r.wordText,
                    wordTranslation = r.wordTranslation,
                )
            }
        }

    override suspend fun getLevelTransitions(): Try<List<LevelTransition>> =
        wordDataSource.getLevelTransitions().map { list ->
            list.map { r ->
                LevelTransition(
                    fromLevel = r.fromLevel,
                    toLevel = r.toLevel,
                    count = r.count,
                )
            }
        }
}
