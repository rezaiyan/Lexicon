@file:OptIn(ExperimentalTime::class)

package domain.word.model

import kotlin.time.ExperimentalTime

/**
 * Domain model for a vocabulary word
 * Pure business logic model without any framework dependencies
 */
data class Word(
    val id: Int,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int = 0,
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val lastReviewDate: Long = 0L,
    val nextReviewDate: Long,
    val dateAdded: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Compare words by content (ignoring ID and learning progress)
     * Two words are considered duplicates if they have the same:
     * - originalWord (case-insensitive)
     * - translation (case-insensitive)
     */
    fun isSameContent(other: Word): Boolean {
        return originalWord.trim().equals(other.originalWord.trim(), ignoreCase = true) &&
                translation.trim().equals(other.translation.trim(), ignoreCase = true)
    }
}

