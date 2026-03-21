package data.analytics.remote

import core.common.Try
import data.analytics.remote.model.AccuracyByLevelResponse
import data.analytics.remote.model.ComebackWordResponse
import data.analytics.remote.model.DayOfWeekAccuracyResponse
import data.analytics.remote.model.DifficultWordResponse
import data.analytics.remote.model.HourlyAccuracyResponse
import data.analytics.remote.model.LanguagePairStatsResponse
import data.analytics.remote.model.MasteredWordResponse
import data.analytics.remote.model.LevelTransitionRemoteResponse
import data.analytics.remote.model.MostReviewedWordResponse

interface IAnalyticsWordDataSource {
    suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<DifficultWordResponse>>
    suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWordResponse>>
    suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevelResponse>>
    suspend fun getAccuracyByHour(): Try<List<HourlyAccuracyResponse>>
    suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracyResponse>>
    suspend fun getWordsMastered(limit: Int): Try<List<MasteredWordResponse>>
    suspend fun getLanguageStats(): Try<List<LanguagePairStatsResponse>>
    suspend fun getComebackWords(): Try<List<ComebackWordResponse>>
    suspend fun getLevelTransitions(): Try<List<LevelTransitionRemoteResponse>>
}
