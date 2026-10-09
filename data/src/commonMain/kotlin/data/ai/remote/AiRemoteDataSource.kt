package data.ai.remote

import data.ai.remote.model.ExtractVocabularyRequest
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.ExtractWordsResponse
import data.ai.remote.model.SuggestWordsRequest
import data.ai.remote.model.SuggestWordsResponse
import data.ai.remote.model.VocabularyExtractionResponse
import data.core.network.client.ApiClient
import core.common.Try
import core.common.fold
import core.error.DomainError
import domain.word.add.service.ImageLimits
import expects.logNetwork
import utils.Language
import kotlin.io.encoding.Base64

/**
 * Remote data source for AI-powered operations
 * Handles vocabulary extraction from images
 */
class AiRemoteDataSource(
    private val apiClient: ApiClient
) : IAiRemoteDataSource {

    override suspend fun extractWords(request: ExtractWordsRequest): Try<ExtractWordsResponse> =
        apiClient.postNotNull("/ai/extract-words", request)

    override suspend fun suggestWords(request: SuggestWordsRequest): Try<SuggestWordsResponse> =
        apiClient.postNotNull("/ai/suggest-vocabulary", request)

    override suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean,
        extractSentences: Boolean
    ): Try<String> {
        if (imageBytes.size > ImageLimits.MAX_UPLOAD_BYTES) {
            return Try.failure(DomainError.AddWords.ImageTooLarge(ImageLimits.MAX_UPLOAD_BYTES))
        }
        if (imageBytes.size < ImageLimits.MIN_UPLOAD_BYTES) {
            return Try.failure(DomainError.AddWords.ImageUnreadable)
        }

        val base64Image = Base64.encode(imageBytes)
        val request = ExtractVocabularyRequest(
            imageBase64 = base64Image,
            targetLanguage = targetLanguage.aiPromptName,
            extractWords = extractWords,
            extractSentences = extractSentences
        )

        logNetwork("AiRemoteDataSource", "Extracting vocabulary from image (${imageBytes.size} bytes)")

        val result = apiClient.postNotNull<VocabularyExtractionResponse>(
            path = "/ai/extract-vocabulary",
            body = request
        )

        return result.fold(
            onSuccess = { response ->
                val extractedText = response.extractedText
                if (extractedText.isBlank()) {
                    Try.failure(DomainError.AddWords.NothingRecognized)
                } else {
                    logNetwork("AiRemoteDataSource", "Successfully extracted vocabulary from image")
                    Try.success(extractedText)
                }
            },
            onFailure = { error ->
                logNetwork("AiRemoteDataSource", "Error extracting vocabulary: ${error.message}")
                // Typed errors pass through so callers can react (premium lapse, offline) without
                // parsing messages.
                if (error is DomainError) {
                    Try.failure(error)
                } else {
                    val userMessage = when {
                        error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "No internet connection. Please check your network."

                        error.message?.contains("timeout", ignoreCase = true) == true ->
                            "Request timed out. Please check your connection and try again."

                        else -> error.message ?: "Service temporarily unavailable. Please try again later."
                    }
                    Try.failure(Exception(userMessage))
                }
            }
        )
    }
}
