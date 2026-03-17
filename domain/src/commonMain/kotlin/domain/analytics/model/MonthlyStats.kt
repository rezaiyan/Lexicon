package domain.analytics.model

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)
