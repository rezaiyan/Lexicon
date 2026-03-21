package data.analytics.remote

import core.common.Try
import data.analytics.remote.model.AccuracyByLevelResponse
import data.analytics.remote.model.ComebackWordResponse
import data.analytics.remote.model.DayOfWeekAccuracyResponse
import data.analytics.remote.model.DifficultWordResponse
import data.analytics.remote.model.HourlyAccuracyResponse
import data.analytics.remote.model.LanguagePairStatsResponse
import data.analytics.remote.model.LevelTransitionRemoteResponse
import data.analytics.remote.model.MasteredWordResponse
import data.analytics.remote.model.MostReviewedWordResponse
import data.core.network.client.ApiClient

class AnalyticsWordRemoteDataSource(
    private val apiClient: ApiClient,
) : IAnalyticsWordDataSource {

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

    override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWordResponse>> =
        apiClient.getNotNull("/analytics/words-mastered?limit=$limit")

    override suspend fun getLanguageStats(): Try<List<LanguagePairStatsResponse>> =
        apiClient.getNotNull("/analytics/language-stats")

    override suspend fun getComebackWords(): Try<List<ComebackWordResponse>> =
        apiClient.getNotNull("/analytics/comeback-words")

    override suspend fun getLevelTransitions(): Try<List<LevelTransitionRemoteResponse>> =
        apiClient.getNotNull("/analytics/level-transitions")
}
