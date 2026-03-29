package domain.wordrush.model

data class WordRushGameRecord(
    val clientGameId: String,
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val bestStreak: Int,
    val durationMs: Long,
    val avgResponseMs: Long,
    val grade: WordRushGrade,
    val livesRemaining: Int,
    val completedNormally: Boolean,
    val playedAt: Long,
)
