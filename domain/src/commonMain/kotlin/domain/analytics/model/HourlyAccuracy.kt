package domain.analytics.model

data class HourlyAccuracy(
    val hour: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)
