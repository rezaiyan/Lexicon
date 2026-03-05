package data.ai.remote

import core.common.Try
import utils.Language

interface IAiRemoteDataSource {
    suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean = true,
        extractSentences: Boolean = false
    ): Try<String>
}
