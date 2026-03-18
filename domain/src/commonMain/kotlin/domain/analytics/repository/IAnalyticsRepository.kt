package domain.analytics.repository

import core.common.Try
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
    suspend fun getWeeklyReport(): Try<WeeklyReport>
    suspend fun syncToBackend(): Try<Int>
}
