package data.ai.remote

import core.common.Try
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.ExtractWordsResponse
import data.ai.remote.model.SuggestWordsRequest
import data.ai.remote.model.SuggestWordsResponse
import utils.Language

interface IAiRemoteDataSource {
    suspend fun extractWords(request: ExtractWordsRequest): Try<ExtractWordsResponse>

    suspend fun suggestWords(request: SuggestWordsRequest): Try<SuggestWordsResponse>

    /** Legacy v1 text contract; kept as a fallback while servers without `/ai/extract-words` exist. */
    suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean = true,
        extractSentences: Boolean = false
    ): Try<String>
}
