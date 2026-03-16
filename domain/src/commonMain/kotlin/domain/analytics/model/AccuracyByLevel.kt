package domain.analytics.model

data class AccuracyByLevel(
    val level: Int,
    val totalReviews: Long,
    val correctCount: Long,
    val accuracyPercent: Double,
)
