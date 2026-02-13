package domain.ai.repository

import domain.common.Try
import utils.Language

interface IAiRepository {
    suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean = true,
        extractSentences: Boolean = false
    ): Try<String>
}

