package domain.analytics.model

data class ResponseTimeTrend(
    val year: Int,
    val week: Int,
    val avgResponseTimeMs: Double,
)
