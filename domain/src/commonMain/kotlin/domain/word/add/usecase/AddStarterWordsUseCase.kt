package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrNull
import core.common.onSuccess
import domain.onboarding.model.SuggestedVocabulary
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.repository.IAddWordsLanguageRepository

/**
 * Adds the onboarding starter words the user accepted, and remembers their language pair as the
 * default for adding words later.
 */
class AddStarterWordsUseCase(
    private val addWords: AddWordsUseCase,
    private val languageRepository: IAddWordsLanguageRepository,
) : UseCase<List<SuggestedVocabulary>, AddWordsOutcome> {

    override suspend fun invoke(params: List<SuggestedVocabulary>): Try<AddWordsOutcome> {
        val first = params.firstOrNull() ?: return Try.success(AddWordsOutcome(added = 0, duplicates = 0))
        val languages = LanguagePair.orNull(learning = first.targetLanguage, native = first.sourceLanguage)
            ?: return Try.success(AddWordsOutcome(added = 0, duplicates = 0))
        val drafts = params.mapNotNull { WordDraft.of(it.originalWord, it.translation, it.description).getOrNull() }
        if (drafts.isEmpty()) return Try.success(AddWordsOutcome(added = 0, duplicates = 0))

        return addWords(AddWordsCommand(drafts, languages, emptySet(), WordOrigin.Onboarding))
            .onSuccess { languageRepository.saveLastUsed(languages) }
    }
}
