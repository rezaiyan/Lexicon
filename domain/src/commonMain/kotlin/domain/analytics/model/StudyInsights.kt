package domain.analytics.model

data class StudyInsights(
    val totalCardsReviewed: Long,
    val totalCorrect: Long,
    val accuracyPercent: Double,
    val totalStudyTimeMs: Long,
    val totalSessions: Long,
    val daysStudied: Long,
    val uniqueWordsReviewed: Long,
    val averageResponseTimeMs: Long?,
    val averageSessionDurationMs: Long?,
    val wordsMasteredCount: Long,
)
