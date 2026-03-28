package domain.wordrush.model

data class WordRushInsights(
    val totalGames: Long,
    val totalCompleted: Long,
    val completionRatePercent: Double,
    val bestStreakEver: Int,
    val avgScore: Double,
    val avgAccuracyPercent: Double,
    val totalTimePlayedMs: Long,
    val avgDurationMs: Double,
    val avgResponseMs: Double,
)
