package domain.word.add.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.getOrNull
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import domain.word.add.model.LanguagePair
import domain.word.add.repository.IAddWordsLanguageRepository
import kotlinx.coroutines.flow.first

/**
 * Best guess for the languages of words the user is about to add, or null when nothing is known
 * (the add-words flow then asks). Order: last used pair → pair matching the learning focus → most common pair.
 */
class ResolveAddWordsLanguagesUseCase(
    private val languageRepository: IAddWordsLanguageRepository,
    private val learningFocusRepository: ILearningFocusRepository,
) : NoParamUseCase<LanguagePair?> {

    override suspend fun invoke(params: Unit): Try<LanguagePair?> = Try {
        val last = languageRepository.lastUsed().getOrNull()
        val common = languageRepository.mostCommon().getOrNull()
        val focus = (learningFocusRepository.observePreference().first() as? LearningFocus.Single)?.language
        val native = last?.native ?: common?.native
        when {
            focus == null -> last ?: common
            last?.learning == focus -> last
            common?.learning == focus -> common
            native != null -> LanguagePair.orNull(focus, native)
            else -> null
        }
    }
}
