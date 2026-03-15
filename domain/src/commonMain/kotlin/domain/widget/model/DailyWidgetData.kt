package domain.widget.model

/**
 * Data model for the daily word widget.
 * Contains the word to display and the user's current streak.
 */
data class DailyWidgetData(
    val word: String,
    val translation: String,
    val streakCount: Int,
    val dueCardCount: Int
)
