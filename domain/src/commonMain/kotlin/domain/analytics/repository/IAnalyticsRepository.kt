package domain.analytics.repository

import core.common.Try
import domain.analytics.model.*

interface IAnalyticsRepository {
    suspend fun getStudyInsights(): Try<StudyInsights>
    suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>>
    suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>>
    suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>>
    suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>>
    suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>>
    suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>>
    suspend fun getRecentSessions(limit: Int): Try<List<StudySession>>
    suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>>
    suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>>
    suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>>
    suspend fun getMonthlyStats(): Try<List<MonthlyStats>>
    suspend fun getComebackWords(): Try<List<ComebackWord>>
    suspend fun syncToBackend(): Try<Int>
}
