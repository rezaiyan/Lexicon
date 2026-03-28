package data.wordrush.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncWordRushRequest(
    val games: List<SyncWordRushGameRequest>,
)

@Serializable
data class SyncWordRushGameRequest(
    val clientGameId: String,
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val bestStreak: Int,
    val durationMs: Long,
    val avgResponseMs: Long,
    val grade: String,
    val livesRemaining: Int,
    val completedNormally: Boolean,
    val playedAt: Long,
)

@Serializable
data class WordRushInsightsResponse(
    val totalGames: Long = 0,
    val totalCompleted: Long = 0,
    val completionRatePercent: Double = 0.0,
    val bestStreakEver: Int = 0,
    val avgScore: Double = 0.0,
    val avgAccuracyPercent: Double = 0.0,
    val totalTimePlayedMs: Long = 0,
    val avgDurationMs: Double = 0.0,
    val avgResponseMs: Double = 0.0,
)
