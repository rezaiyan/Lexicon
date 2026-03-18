package domain.ai.usecase

import core.common.FlowUseCase
import core.common.fold
import core.common.getOrThrow
import domain.ai.repository.IAiRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import utils.Language

class ImportFromImageUseCase(
    private val aiRepository: IAiRepository,
    private val importWordsUseCase: ImportWordsUseCase,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
    ) : FlowUseCase<ImportFromImageUseCase.Params, ImportImageResult> {
    data class Params(
        val imageBytes: ByteArray,
        val extractWords: Boolean = true,
        val extractSentences: Boolean = false,
        val sourceLanguage: Language? = null,
    )

    override operator fun invoke(params: Params) = invoke(
        params.imageBytes,
        params.extractWords,
        params.extractSentences,
        params.sourceLanguage,
    )

    operator fun invoke(
        imageBytes: ByteArray,
        extractWords: Boolean = true,
        extractSentences: Boolean = false,
        sourceLanguage: Language? = null,
    ): Flow<ImportImageResult> = flow {
        emit(ImportImageResult.Loading)

        val targetLanguage = getCurrentLanguageUseCase.invoke().getOrThrow()
        val extractionResult = aiRepository.extractVocabularyFromImage(
            imageBytes,
            targetLanguage,
            extractWords,
            extractSentences
        )

        extractionResult.fold(
            onSuccess = { extractedText ->
                importWordsUseCase(extractedText, sourceLanguage = targetLanguage, targetLanguage = sourceLanguage).fold(
                    onSuccess = { count -> emit(ImportImageResult.Success(count)) },
                    onFailure = { error -> emit(ImportImageResult.Error(error.message ?: "Import failed")) }
                )
            },
            onFailure = { error: Throwable ->
                emit(ImportImageResult.Error(error.message ?: "Failed to extract vocabulary from image"))
            }
        )
    }
}

sealed class ImportImageResult {
    data object Loading : ImportImageResult()
    data class Success(val count: Int) : ImportImageResult()
    data class Error(val message: String) : ImportImageResult()
}
