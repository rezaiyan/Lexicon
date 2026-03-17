package domain.analytics.model

data class WordDifficulty(
    val wordId: Int,
    val wordText: String,
    val wordTranslation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val totalReviews: Int,
    val errorCount: Int,
    val errorRate: Double,
)
