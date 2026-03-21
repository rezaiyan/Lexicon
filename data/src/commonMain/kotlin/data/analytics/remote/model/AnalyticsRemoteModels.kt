package data.analytics.remote.model

import kotlinx.serialization.Serializable

// === Sync (client -> server) ===

@Serializable
data class SyncAnalyticsRequest(
    val sessions: List<SyncSessionRequest>,
)

@Serializable
data class SyncSessionRequest(
    val clientSessionId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long,
    val totalCards: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val reviewType: String,
    val completedNormally: Boolean,
    val events: List<SyncReviewEventRequest>,
)

@Serializable
data class SyncReviewEventRequest(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val rating: Int,
    val previousLevel: Int,
    val newLevel: Int,
    val responseTimeMs: Long,
    val reviewedAt: Long,
)

@Serializable
data class SyncAnalyticsResponse(
    val syncedSessionIds: List<String>,
)

// === Query responses (server -> client) ===

@Serializable
data class StudyInsightsResponse(
    val totalCardsReviewed: Long = 0,
    val totalCorrect: Long = 0,
    val accuracyPercent: Double = 0.0,
    val totalStudyTimeMs: Long = 0,
    val totalSessions: Long = 0,
    val daysStudied: Long = 0,
    val uniqueWordsReviewed: Long = 0,
    val averageResponseTimeMs: Long? = null,
    val averageSessionDurationMs: Long? = null,
    val sessionCompletionRate: Double? = null,
    val wordsMasteredCount: Long = 0,
)

@Serializable
data class DifficultWordResponse(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val totalReviews: Int,
    val errorCount: Int,
    val errorRate: Double,
)

@Serializable
data class MostReviewedWordResponse(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val totalReviews: Int,
)

@Serializable
data class AccuracyByLevelResponse(
    val level: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)

@Serializable
data class HourlyAccuracyResponse(
    val hour: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)

@Serializable
data class DayOfWeekAccuracyResponse(
    val dayOfWeek: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)

@Serializable
data class StudySessionResponse(
    val clientSessionId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long,
    val totalCards: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val reviewType: String,
    val completedNormally: Boolean,
)

@Serializable
data class HeatmapDayResponse(
    val date: String,
    val count: Int,
)

@Serializable
data class MasteredWordResponse(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val masteredAt: Long,
)

@Serializable
data class LanguagePairStatsResponse(
    val sourceLanguage: String,
    val targetLanguage: String,
    val totalReviews: Long,
    val correctCount: Long,
    val uniqueWords: Long,
    val accuracyPercent: Double,
)

@Serializable
data class MonthlyStatsResponse(
    val year: Int,
    val month: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)

@Serializable
data class ComebackWordResponse(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
)

@Serializable
data class LevelTransitionRemoteResponse(
    val fromLevel: Int,
    val toLevel: Int,
    val count: Long,
)

@Serializable
data class ResponseTimeTrendRemoteResponse(
    val year: Int,
    val week: Int,
    val avgResponseTimeMs: Double,
)

// === Daily Stats ===

@Serializable
data class DailyStatsRemoteResponse(
    val date: String = "",
    val sessionsCount: Int = 0,
    val cardsReviewed: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val studyTimeMs: Long = 0,
    val uniqueWordsReviewed: Int = 0,
    val wordsLeveledUp: Int = 0,
    val wordsLeveledDown: Int = 0,
)

// === Weekly Report ===

@Serializable
data class WeeklyReportRemoteResponse(
    val cardsReviewed: Int = 0,
    val previousWeekCardsReviewed: Int = 0,
    val changePercent: Double? = null,
    val accuracyPercent: Double = 0.0,
    val wordsMastered: Int = 0,
    val totalStudyTimeMs: Long = 0,
    val sessionsCount: Int = 0,
    val bestDay: BestDayRemoteResponse? = null,
    val weekStartDate: String = "",
    val weekEndDate: String = "",
)

@Serializable
data class BestDayRemoteResponse(
    val dayName: String = "",
    val cardsReviewed: Int = 0,
    val accuracyPercent: Double = 0.0,
)
