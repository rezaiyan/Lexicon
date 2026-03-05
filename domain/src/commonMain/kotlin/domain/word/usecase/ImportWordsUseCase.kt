package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.fold
import core.common.getOrDefault
import core.common.getOrThrow
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.repository.IWordRepository
import domain.word.service.IImportValidationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import utils.Language

class ImportWordsUseCase(
    private val wordRepository: IWordRepository,
    private val validationService: IImportValidationService,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase
) : UseCase<ImportWordsUseCase.Params, Int> {

    data class Params(
        val text: String,
        val sourceLanguage: Language? = null,
        val targetLanguage: Language? = null,
    )

    override suspend operator fun invoke(params: Params): Try<Int> =
        invoke(params.text, params.sourceLanguage, params.targetLanguage)

    suspend operator fun invoke(
        text: String,
        sourceLanguage: Language? = null,
        targetLanguage: Language? = null,
    ): Try<Int> {
        val resolvedSourceLanguage = sourceLanguage ?: Language.ENGLISH
        val resolvedTargetLanguage = targetLanguage ?: getCurrentLanguageUseCase().getOrDefault(Language.ENGLISH)
        return validationService.validateAndParse(
            text = text,
            sourceLanguage = resolvedSourceLanguage,
            targetLanguage = resolvedTargetLanguage
        ).fold(
            onSuccess = { parsedWords ->
                val importWords = parsedWords.distinctBy {
                    Pair(
                        it.originalWord.trim().lowercase(),
                        it.translation.trim().lowercase()
                    )
                }

                wordRepository.insertWords(importWords).flatMap { importedCount ->
                    if (importedCount > 0) {
                        Try.success(importedCount)
                    } else {
                        Try.failure(Exception("All ${importWords.size} word(s) already exist in your collection."))
                    }
                }
            },
            onFailure = { throwable ->
                Try.failure(throwable)
            }
        )
    }

    fun asFlow(text: String): Flow<Int> {
        val parseResult = validationService.validateAndParse(text)
        val parsedWords = parseResult.getOrThrow()

        val uniqueImportWords = parsedWords.distinctBy {
            Pair(
                it.originalWord.trim().lowercase(),
                it.translation.trim().lowercase()
            )
        }

        return wordRepository.getAllWords()
            .flatMapLatest { existingWords ->
                flow {
                    val newWords = uniqueImportWords.filter { newWord ->
                        existingWords.none { existingWord ->
                            existingWord.isSameContent(newWord)
                        }
                    }

                    if (newWords.isEmpty()) {
                        throw Exception("All ${uniqueImportWords.size} word(s) already exist in your collection.")
                    } else {
                        val count = wordRepository.insertWords(newWords).getOrThrow()
                        emit(count)
                    }
                }
            }
    }
}
