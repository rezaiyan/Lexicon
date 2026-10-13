package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.onSuccess
import core.error.DomainError
import domain.ai.model.AiGuidance
import domain.ai.repository.IAiRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.repository.ISuggestionLevelRepository

/**
 * AI-generated words for a level and topics. Spends AI credits server-side (refunded when nothing
 * is suggested); fails with `InsufficientCredits` when the balance is short, and with
 * `InstructionsRefused` when the learner's instructions can't be used.
 * Returns drafts for review; no words are saved. The level is remembered on success, so the next
 * request starts from it.
 */
class SuggestWordsUseCase(
    private val aiRepository: IAiRepository,
    private val levelRepository: ISuggestionLevelRepository,
) : UseCase<SuggestWordsUseCase.Params, List<WordDraft>> {

    data class Params(
        val languages: LanguagePair,
        val level: ProficiencyLevel,
        val topics: List<String>,
        val guidance: AiGuidance = AiGuidance(),
    )

    override suspend fun invoke(params: Params): Try<List<WordDraft>> =
        aiRepository.suggestWords(params.languages, params.level, params.topics, params.guidance).flatMap { drafts ->
            if (drafts.isEmpty()) Try.failure(DomainError.AddWords.NothingRecognized) else Try.success(drafts)
        }.onSuccess { levelRepository.saveLastUsed(params.level) }
}
