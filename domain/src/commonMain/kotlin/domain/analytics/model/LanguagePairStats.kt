package domain.analytics.model

data class LanguagePairStats(
    val sourceLanguage: String,
    val targetLanguage: String,
    val totalReviews: Long,
    val correctCount: Long,
    val uniqueWords: Long,
    val accuracyPercent: Double,
)
