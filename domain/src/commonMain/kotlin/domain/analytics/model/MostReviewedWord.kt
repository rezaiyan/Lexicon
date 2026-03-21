package domain.analytics.model

data class MostReviewedWord(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val totalReviews: Int,
)
