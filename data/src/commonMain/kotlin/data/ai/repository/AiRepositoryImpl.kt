package data.ai.repository

import data.ai.remote.IAiRemoteDataSource
import domain.ai.repository.IAiRepository
import core.common.Try
import utils.Language

class AiRepositoryImpl(
    private val aiRemoteDataSource: IAiRemoteDataSource
) : IAiRepository {

    override suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean,
        extractSentences: Boolean
    ): Try<String> {
        return aiRemoteDataSource.extractVocabularyFromImage(
            imageBytes,
            targetLanguage,
            extractWords,
            extractSentences
        )
    }
}

