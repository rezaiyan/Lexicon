package data.ai.repository

import data.ai.remote.AiRemoteDataSource
import domain.ai.repository.IAiRepository
import utils.Language

class AiRepositoryImpl(
    private val aiRemoteDataSource: AiRemoteDataSource
) : IAiRepository {

    override suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean,
        extractSentences: Boolean
    ): Result<String> {
        return aiRemoteDataSource.extractVocabularyFromImage(
            imageBytes,
            targetLanguage,
            extractWords,
            extractSentences
        )
    }
}

