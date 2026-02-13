@file:OptIn(ExperimentalTime::class)

package domain.word.service

import domain.common.Try
import domain.word.model.Word
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface IImportValidationService {
    fun validateAndParse(text: String): Try<List<Word>>
}

class ImportValidationService : IImportValidationService {

    override fun validateAndParse(text: String): Try<List<Word>> {
        val trimmed = text.trim()

        return when {
            trimmed.isEmpty() ->
                Try.failure(Exception("File is empty"))

            !isValidFormat(trimmed) ->
                Try.failure(
                    Exception("File content doesn't match expected format. Expected: word,translation or word,translation,description")
                )

            else -> {
                val parsed = parseImportText(trimmed)

                if (parsed.isEmpty()) {
                    Try.failure(
                        Exception("No valid words found. Check format: word,translation")
                    )
                } else {
                    Try.success(parsed)
                }
            }
        }
    }

    private fun isValidFormat(text: String): Boolean {
        if (!text.contains(",")) return false

        val lines = text.split(Regex("[;\n]+")).map { it.trim() }.filter { it.isNotBlank() }
        return lines.any { line ->
            if (line.startsWith("#")) return@any false
            val parts = line.split(",")
            parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
        }
    }

    private fun parseImportText(text: String): List<Word> {
        val words = mutableListOf<Word>()
        val entries = text.trim()
            .split(Regex("[;\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        entries.forEach { entry ->
            if (entry.isBlank() || entry.startsWith("#")) return@forEach

            val parts = entry.split(",", limit = 3).map { it.trim() }
            if (parts.size < 2) return@forEach

            val originalWord = parts[0]
            val translation = parts[1]
            val description = if (parts.size > 2) parts[2] else ""

            if (originalWord.isNotBlank() && translation.isNotBlank()) {
                @OptIn(ExperimentalTime::class)
                val nextReviewTime = Clock.System.now().toEpochMilliseconds() - 1000
                words.add(
                    Word(
                        id = 0,
                        originalWord = originalWord,
                        translation = translation,
                        description = description,
                        sourceLanguage = "",
                        targetLanguage = "",
                        level = 0,
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        lastReviewDate = 0L,
                        nextReviewDate = nextReviewTime
                    )
                )
            }
        }

        return words
    }
}
