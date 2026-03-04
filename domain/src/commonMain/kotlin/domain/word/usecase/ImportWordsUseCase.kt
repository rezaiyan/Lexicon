package domain.word.usecase

import core.common.Try
import core.common.fold
import core.common.getOrDefault
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
) {

    suspend fun execute(
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

                val importedWords = wordRepository.insertWords(importWords)
                if (importedWords > 0) {
                    Try.success(importWords.size)
                } else {
                    Try.failure(Exception("All ${importWords.size} word(s) already exist in your collection."))
                }
            },
            onFailure = { throwable ->
                Try.failure(throwable)
            }
        )
    }

    operator fun invoke(text: String): Flow<Try<Int>> =
        validationService.validateAndParse(text)
            .fold(
                onSuccess = { parsedWords ->
                    val uniqueImportWords = parsedWords.distinctBy {
                        Pair(
                            it.originalWord.trim().lowercase(),
                            it.translation.trim().lowercase()
                        )
                    }

                    wordRepository.getAllWords()
                        .flatMapLatest { existingWords ->
                            flow {
                                val newWords = uniqueImportWords.filter { newWord ->
                                    existingWords.none { existingWord ->
                                        existingWord.isSameContent(newWord)
                                    }
                                }

                                if (newWords.isEmpty()) {
                                    emit(Try.failure(Exception("All ${uniqueImportWords.size} word(s) already exist in your collection.")))
                                } else {
                                    wordRepository.insertWords(newWords)
                                    emit(Try.success(newWords.size))
                                }
                            }
                        }
                },
                onFailure = { throwable ->
                    flow { emit(Try.failure(throwable)) }
                }
            )
}
