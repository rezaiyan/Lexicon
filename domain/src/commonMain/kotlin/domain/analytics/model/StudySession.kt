package domain.analytics.model

data class StudySession(
    val sessionId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long,
    val totalCards: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val reviewType: String,
    val completedNormally: Boolean,
)
