package domain.ai.usecase

import core.common.FlowUseCase
import core.common.fold
import core.common.getOrThrow
import domain.ai.repository.IAiRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExtractVocabularyFromImageUseCase(
    private val aiRepository: IAiRepository,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase,
) : FlowUseCase<ExtractVocabularyFromImageUseCase.Params, ExtractVocabularyResult> {

    data class Params(
        val imageBytes: ByteArray,
        val extractWords: Boolean = true,
        val extractSentences: Boolean = false,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false
            return imageBytes.contentEquals(other.imageBytes) &&
                extractWords == other.extractWords &&
                extractSentences == other.extractSentences
        }

        override fun hashCode(): Int {
            var result = imageBytes.contentHashCode()
            result = 31 * result + extractWords.hashCode()
            result = 31 * result + extractSentences.hashCode()
            return result
        }
    }

    override operator fun invoke(params: Params) = invoke(
        params.imageBytes,
        params.extractWords,
        params.extractSentences,
    )

    operator fun invoke(
        imageBytes: ByteArray,
        extractWords: Boolean = true,
        extractSentences: Boolean = false,
    ): Flow<ExtractVocabularyResult> = flow {
        emit(ExtractVocabularyResult.Loading)

        val targetLanguage = getCurrentLanguageUseCase.invoke().getOrThrow()
        val extractionResult = aiRepository.extractVocabularyFromImage(
            imageBytes,
            targetLanguage,
            extractWords,
            extractSentences,
        )

        extractionResult.fold(
            onSuccess = { csvText -> emit(ExtractVocabularyResult.Success(csvText)) },
            onFailure = { error ->
                emit(ExtractVocabularyResult.Error(error.message ?: "Failed to extract vocabulary from image"))
            }
        )
    }
}

sealed class ExtractVocabularyResult {
    data object Loading : ExtractVocabularyResult()
    data class Success(val csvText: String) : ExtractVocabularyResult()
    data class Error(val message: String) : ExtractVocabularyResult()
}
