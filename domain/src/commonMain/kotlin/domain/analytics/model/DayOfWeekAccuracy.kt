package domain.analytics.model

data class DayOfWeekAccuracy(
    val dayOfWeek: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)
