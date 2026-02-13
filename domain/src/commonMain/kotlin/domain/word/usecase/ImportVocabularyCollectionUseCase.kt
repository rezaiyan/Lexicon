@file:OptIn(ExperimentalTime::class)

package domain.word.usecase

import domain.common.Try
import domain.word.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Parse vocabulary collection content and convert to Word list
 * Supports various formats:
 * - "word,translation;word2,translation2" (semicolon-separated on same line)
 * - "word : translation"
 * - "word : translation : description"
 * - "word,translation"
 * - "word,translation,description"
 * - Tab-separated format
 */
class ImportVocabularyCollectionUseCase {

    suspend fun parseCollection(content: String, targetLanguage: String, sourceLanguage: String): Try<List<Word>> = withContext(Dispatchers.Default) {
        Try {
            val words = mutableListOf<Word>()
            val lines = content.trim().lines()

            var wordId = 0

            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    return@forEach // Skip empty lines and comments
                }

                // Handle semicolon-separated entries (multiple word pairs on one line)
                // Format: "word1,translation1;word2,translation2;..."
                if (trimmed.contains(";")) {
                    val pairs = trimmed.split(";")
                    pairs.forEach { pair ->
                        val word = parseLine(pair.trim(), wordId++, targetLanguage, sourceLanguage)
                        if (word != null) {
                            words.add(word)
                        }
                    }
                } else {
                    val word = parseLine(trimmed, wordId++, targetLanguage, sourceLanguage)
                    if (word != null) {
                        words.add(word)
                    }
                }
            }

            if (words.isEmpty()) {
                throw Exception("No valid words found in the collection")
            }

            words.toList()
        }
    }

    private fun parseLine(line: String, id: Int, targetLanguage: String, sourceLanguage: String): Word? {
        // Handle semicolon-separated format (most common for vocabulary files)
        // Format: "word,translation;word2,translation2;..."
        if (line.contains(";")) {
            // This is a single line with multiple word pairs separated by semicolons
            val pairs = line.split(";")
            return null // Skip this, caller should handle individual pairs
        }

        // Try colon-separated format: "word : translation : description"
        if (line.contains(":") && !line.startsWith("http")) {
            val colonParts = line.split(":").map { it.trim() }
            if (colonParts.size >= 2) {
                return Word(
                    id = id,
                    originalWord = colonParts[0],
                    translation = colonParts[1],
                    description = colonParts.getOrNull(2) ?: "",
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    level = 0,
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    lastReviewDate = 0L,
                    nextReviewDate = Clock.System.now().toEpochMilliseconds()
                )
            }
        }

        // Try comma-separated format: "word,translation"
        if (line.contains(",")) {
            val commaParts = line.split(",").map { it.trim() }
            if (commaParts.size >= 2) {
                return Word(
                    id = id,
                    originalWord = commaParts[0],
                    translation = commaParts[1],
                    description = commaParts.getOrNull(2) ?: "",
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    level = 0,
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    lastReviewDate = 0L,
                    nextReviewDate = Clock.System.now().toEpochMilliseconds()
                )
            }
        }

        // Try tab-separated format
        if (line.contains("\t")) {
            val tabParts = line.split("\t").map { it.trim() }
            if (tabParts.size >= 2) {
                return Word(
                    id = id,
                    originalWord = tabParts[0],
                    translation = tabParts[1],
                    description = tabParts.getOrNull(2) ?: "",
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    level = 0,
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    lastReviewDate = 0L,
                    nextReviewDate = Clock.System.now().toEpochMilliseconds()
                )
            }
        }

        // Couldn't parse the line
        return null
    }
}
