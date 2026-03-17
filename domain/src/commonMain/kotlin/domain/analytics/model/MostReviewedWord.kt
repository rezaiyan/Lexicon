package domain.analytics.model

data class MostReviewedWord(
    val wordId: Int,
    val wordText: String,
    val wordTranslation: String,
    val totalReviews: Int,
)
