package domain.word.model

/**
 * Rich result from [domain.word.usecase.ReviewWordUseCase].
 *
 * Carries the updated word (with new SRS fields already persisted) and the
 * level that was in effect before the review, so callers can detect level-up
 * and mastery events without duplicating the SRS computation.
 */
data class ReviewWordResult(
    val updatedWord: Word,
    val previousLevel: Int,
) {
    val newLevel: Int get() = updatedWord.level
    val wasLevelUp: Boolean get() = newLevel > previousLevel
    val wasMastered: Boolean get() = newLevel == 6 && previousLevel < 6
}
