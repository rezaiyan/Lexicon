package data.ai.remote

import data.ai.remote.model.ExtractVocabularyRequest
import data.ai.remote.model.VocabularyExtractionResponse
import data.core.network.client.ApiClient
import expects.logNetwork
import utils.Language
import kotlin.io.encoding.Base64

/**
 * Remote data source for AI-powered operations
 * Handles vocabulary extraction from images
 */
class AiRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun extractVocabularyFromImage(
        imageBytes: ByteArray,
        targetLanguage: Language,
        extractWords: Boolean = true,
        extractSentences: Boolean = false
    ): Result<String> {
        // Validate image size
        val maxSizeBytes = 3 * 1024 * 1024
        if (imageBytes.size > maxSizeBytes) {
            return Result.failure(Exception("Image too large. Maximum size is 5MB. Please use a smaller image."))
        }

        if (imageBytes.size < 128) {
            return Result.failure(Exception("Image too small or corrupted. Please try a different image."))
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

        return when {
            result.isSuccess -> {
                val response = result.getOrNull()
                if (response != null) {
                    val extractedText = response.extractedText
                    if (extractedText.isEmpty()) {
                        Result.failure<String>(Exception("No vocabulary found in the image. Please use an image with visible text."))
                    } else {
                        logNetwork("AiRemoteDataSource", "Successfully extracted vocabulary from image")
                        Result.success(extractedText)
                    }
                } else {
                    Result.failure(Exception("No vocabulary found in the image. Please use an image with visible text."))
                }
            }

            else -> {
                val error = result.exceptionOrNull() ?: Exception("Unknown error")
                logNetwork("AiRemoteDataSource", "Error extracting vocabulary: ${error.message}")
                val userMessage = when {
                    error.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "No internet connection. Please check your network."

                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please check your connection and try again."

                    else -> error.message ?: "Service temporarily unavailable. Please try again later."
                }
                Result.failure<String>(Exception(userMessage))
            }
        }
    }
}

