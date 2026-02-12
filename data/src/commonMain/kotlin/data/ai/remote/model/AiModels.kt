package data.ai.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractVocabularyRequest(
    val imageBase64: String,
    val targetLanguage: String,
    val extractWords: Boolean = true,
    val extractSentences: Boolean = false
)

@Serializable
data class VocabularyExtractionResponse(
    val extractedText: String,
    val wordCount: Int,
    val aiExtractionUsageCount: Int = 0,
    val aiExtractionUsageLimit: Int = 10,
    val remainingAiExtractions: Int = 10
)

