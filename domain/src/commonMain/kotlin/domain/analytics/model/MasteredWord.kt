package domain.analytics.model

data class MasteredWord(
    val wordId: Int,
    val wordText: String,
    val wordTranslation: String,
    val masteredAt: Long,
)
