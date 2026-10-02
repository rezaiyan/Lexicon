package domain.word.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.getOrNull
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.first
import utils.Language

/**
 * Default learning (source) language for new words: the focused language,
 * else the most common one in the user's collection, else ENGLISH.
 */
class GetSourceLanguageUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : NoParamUseCase<Language> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Language> = Try {
        when (val focus = observeLearningFocus().first()) {
            is LearningFocus.Single -> focus.language
            LearningFocus.All -> {
                val code = wordRepository.getMostCommonSourceLanguage().getOrNull()
                if (code != null) Language.fromCode(code) else Language.ENGLISH
            }
        }
    }
}
