package domain.analytics.model

data class DailyStudyStats(
    val date: String,
    val sessionsCount: Int,
    val cardsReviewed: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val studyTimeMs: Long,
    val uniqueWordsReviewed: Int,
    val wordsLeveledUp: Int,
    val wordsLeveledDown: Int,
)
