package domain.analytics.model

data class WeeklyReport(
    val cardsReviewed: Int,
    val previousWeekCardsReviewed: Int,
    val changePercent: Double?,
    val accuracyPercent: Double,
    val wordsMastered: Int,
    val totalStudyTimeMs: Long,
    val sessionsCount: Int,
    val bestDay: BestDay?,
    val weekStartDate: String,
    val weekEndDate: String,
)

data class BestDay(
    val dayName: String,
    val cardsReviewed: Int,
    val accuracyPercent: Double,
)
