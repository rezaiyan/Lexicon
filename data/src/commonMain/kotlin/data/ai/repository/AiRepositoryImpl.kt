package data.ai.repository

import core.common.Try
import core.common.getOrNull
import core.common.map
import data.ai.remote.IAiRemoteDataSource
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.SuggestWordsRequest
import data.onboarding.remote.model.toApiValue
import domain.ai.repository.IAiRepository
import domain.credits.ICreditsRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import kotlin.io.encoding.Base64

/**
 * Both calls spend AI credits on the server (or refund, or refuse with 402), so each one ends by
 * telling [credits] the balance changed: every screen showing it updates without asking.
 */
class AiRepositoryImpl(
    private val aiRemoteDataSource: IAiRemoteDataSource,
    private val credits: ICreditsRepository,
) : IAiRepository {

    override suspend fun extractWords(image: ByteArray, languages: LanguagePair): Try<List<WordDraft>> {
        val request = ExtractWordsRequest(
            imageBase64 = Base64.encode(image),
            learningLanguage = languages.learning.aiPromptName,
            nativeLanguage = languages.native.aiPromptName,
        )
        return aiRemoteDataSource.extractWords(request)
            .also { credits.invalidate() }
            .map { body -> body.items.mapNotNull { WordDraft.of(it.term, it.translation, it.note).getOrNull() } }
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
    )
        .also { credits.invalidate() }
        .map { response ->
            response.items.mapNotNull { WordDraft.of(it.originalWord, it.translation, it.description).getOrNull() }
        }
}
