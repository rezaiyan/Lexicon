package domain.analytics.model

data class ReviewEventParams(
    val sessionId: String,
    val wordId: Int,
    val wordText: String,
    val wordTranslation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val rating: Int,
    val previousLevel: Int,
    val newLevel: Int,
    val responseTimeMs: Long,
    val reviewedAt: Long,
)
