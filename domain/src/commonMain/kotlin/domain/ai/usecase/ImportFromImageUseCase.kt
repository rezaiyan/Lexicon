package domain.ai.usecase

import core.common.FlowUseCase
import domain.ai.repository.IAiRepository
import core.common.Try
import core.common.fold
import core.common.getOrThrow
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ImportFromImageUseCase(
    private val aiRepository: IAiRepository,
    private val importWordsUseCase: ImportWordsUseCase,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
    ) : FlowUseCase<ImportFromImageUseCase.Params, ImportImageResult> {
    data class Params(val imageBytes: ByteArray, val extractWords: Boolean = true, val extractSentences: Boolean = false)

    override operator fun invoke(params: Params) = invoke(params.imageBytes, params.extractWords, params.extractSentences)

    operator fun invoke(
        imageBytes: ByteArray,
        extractWords: Boolean = true,
        extractSentences: Boolean = false
    ): Flow<ImportImageResult> = flow {
        emit(ImportImageResult.Loading)
    }.flatMapLatest {
        val targetLanguage = getCurrentLanguageUseCase.invoke().getOrThrow()
        val extractionResult = aiRepository.extractVocabularyFromImage(
            imageBytes,
            targetLanguage,
            extractWords,
            extractSentences
        )

        extractionResult.fold(
            onSuccess = { extractedText ->
                importWordsUseCase(extractedText)
                    .map { tryResult: Try<Int> ->
                        tryResult.fold(
                            onSuccess = { count ->
                                ImportImageResult.Success(count)
                            },
                            onFailure = { error ->
                                ImportImageResult.Error(error.message ?: "Import failed")
                            }
                        )
                    }
            },
            onFailure = { error: Throwable ->
                flow { emit(ImportImageResult.Error(error.message ?: "Failed to extract vocabulary from image")) }
            }
        )
    }
}

sealed class ImportImageResult {
    data object Loading : ImportImageResult()
    data class Success(val count: Int) : ImportImageResult()
    data class Error(val message: String) : ImportImageResult()
}
