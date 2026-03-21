package domain.analytics.repository

import core.common.Try
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.ComebackWord
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.LanguagePairStats
import domain.analytics.model.MasteredWord
import domain.analytics.model.MostReviewedWord
import domain.analytics.model.LevelTransition
import domain.analytics.model.WordDifficulty

interface IAnalyticsWordRepository {
    suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>>
    suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>>
    suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>>
    suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>>
    suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>>
    suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>>
    suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>>
    suspend fun getComebackWords(): Try<List<ComebackWord>>
    suspend fun getLevelTransitions(): Try<List<LevelTransition>>
}
