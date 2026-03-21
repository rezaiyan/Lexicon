package domain.analytics.model

data class MasteredWord(
    val wordId: Long,
    val wordText: String,
    val wordTranslation: String,
    val masteredAt: Long,
)
