package data.ai.repository

import core.common.Try
import core.common.getOrNull
import core.common.map
import core.error.DomainError
import data.ai.remote.IAiRemoteDataSource
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.SuggestWordsRequest
import data.onboarding.remote.model.toApiValue
import domain.ai.repository.IAiRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.parser.VocabularyTextParser
import kotlin.io.encoding.Base64

class AiRepositoryImpl(
    private val aiRemoteDataSource: IAiRemoteDataSource
) : IAiRepository {

    override suspend fun extractWords(image: ByteArray, languages: LanguagePair): Try<List<WordDraft>> {
        val request = ExtractWordsRequest(
            imageBase64 = Base64.encode(image),
            learningLanguage = languages.learning.aiPromptName,
            nativeLanguage = languages.native.aiPromptName,
        )
        val response = aiRemoteDataSource.extractWords(request)
        if (response is Try.Failure && response.throwable.isMissingEndpoint()) return extractWordsV1(image, languages)
        return response.map { body ->
            body.items.mapNotNull { WordDraft.of(it.term, it.translation, it.note).getOrNull() }
        }
    }

    override suspend fun suggestWords(
        languages: LanguagePair,
        level: ProficiencyLevel,
        topics: List<String>,
    ): Try<List<WordDraft>> = aiRemoteDataSource.suggestWords(
        SuggestWordsRequest(
            targetLanguage = languages.learning.displayName,
            nativeLanguage = languages.native.displayName,
            currentLevel = level.toApiValue(),
            interests = topics,
            targetLanguageCode = languages.learning.code,
        ),
    ).map { response ->
        response.items.mapNotNull { WordDraft.of(it.originalWord, it.translation, it.description).getOrNull() }
    }

    /** Servers older than `/ai/extract-words`: v1 text, where "targetLanguage" means the translation language. */
    private suspend fun extractWordsV1(image: ByteArray, languages: LanguagePair): Try<List<WordDraft>> =
        aiRemoteDataSource.extractVocabularyFromImage(
            imageBytes = image,
            targetLanguage = languages.native,
            extractWords = true,
            extractSentences = false,
        ).map { text -> VocabularyTextParser.parse(text).drafts }

    private fun Throwable.isMissingEndpoint() = this is DomainError.Network.ServerError && code == HTTP_NOT_FOUND

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
