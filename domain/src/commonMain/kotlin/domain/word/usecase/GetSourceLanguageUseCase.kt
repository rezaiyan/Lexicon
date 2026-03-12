package domain.word.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.getOrNull
import domain.word.repository.IWordRepository
import utils.Language

/**
 * Returns the user's most common source (original/native) language
 * based on their existing word collection.
 * Falls back to ENGLISH if no words exist.
 */
class GetSourceLanguageUseCase(
    private val wordRepository: IWordRepository
) : NoParamUseCase<Language> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Language> = Try {
        val code = wordRepository.getMostCommonSourceLanguage().getOrNull()
        if (code != null) Language.fromCode(code) else Language.ENGLISH
    }
}
