package domain.word.usecase

import domain.word.repository.IWordRepository
import domain.word.service.IImportValidationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class ImportWordsUseCase(
    private val wordRepository: IWordRepository,
    private val validationService: IImportValidationService
) {

    suspend fun execute(text: String): ImportResult =
        validationService.validateAndParse(text)
            .let { validationResult ->
                when (validationResult) {
                    is IImportValidationService.ValidationResult.Error -> {
                        return@let ImportResult.Error(validationResult.message)
                    }

                    is IImportValidationService.ValidationResult.Success -> {
                        val parsedWords = validationResult.words
                        val importWords = parsedWords.distinctBy {
                            Pair(
                                it.originalWord.trim().lowercase(),
                                it.translation.trim().lowercase()
                            )
                        }

                        val importedWords = wordRepository.insertWords(importWords)
                        if (importedWords > 0) {
                            ImportResult.Success(importWords.size)
                        } else {
                            ImportResult.Error("All ${importWords.size} word(s) already exist in your collection.")
                        }

                    }
                }
            }


    operator fun invoke(text: String): Flow<ImportResult> =
        validationService.validateAndParse(text)
            .let { validationResult ->
                when (validationResult) {
                    is IImportValidationService.ValidationResult.Error -> {
                        flow { emit(ImportResult.Error(validationResult.message)) }
                    }

                    is IImportValidationService.ValidationResult.Success -> {
                        val parsedWords = validationResult.words
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
                                        emit(ImportResult.Error("All ${uniqueImportWords.size} word(s) already exist in your collection."))
                                    } else {
                                        wordRepository.insertWords(newWords)
                                        emit(ImportResult.Success(newWords.size))
                                    }
                                }
                            }
                    }
                }
            }

    sealed class ImportResult {
        data class Success(val count: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}