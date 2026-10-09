package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.error.DomainError
import domain.ai.repository.IAiRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft

/** AI-generated words for a level and topics (premium). Returns drafts for review; nothing is saved. */
class SuggestWordsUseCase(
    private val aiRepository: IAiRepository,
) : UseCase<SuggestWordsUseCase.Params, List<WordDraft>> {

    data class Params(
        val languages: LanguagePair,
        val level: ProficiencyLevel,
        val topics: List<String>,
    )

    override suspend fun invoke(params: Params): Try<List<WordDraft>> =
        aiRepository.suggestWords(params.languages, params.level, params.topics).flatMap { drafts ->
            if (drafts.isEmpty()) Try.failure(DomainError.AddWords.NothingRecognized) else Try.success(drafts)
        }
}
