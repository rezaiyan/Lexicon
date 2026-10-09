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
    val wordCount: Int
)


/** v2 photo extraction (`POST /ai/extract-words`): explicit language pair, structured items. */
@Serializable
data class ExtractWordsRequest(
    val imageBase64: String,
    val learningLanguage: String,
    val nativeLanguage: String,
    val includePhrases: Boolean = false,
)

@Serializable
data class ExtractedWordDto(
    val term: String,
    val translation: String,
    val note: String = "",
)

@Serializable
data class ExtractWordsResponse(val items: List<ExtractedWordDto> = emptyList())

/** `POST /ai/suggest-vocabulary`: signed-in suggestions, skipping words the user already has. */
@Serializable
data class SuggestWordsRequest(
    val targetLanguage: String,
    val nativeLanguage: String,
    val currentLevel: String,
    val interests: List<String> = emptyList(),
    val targetLanguageCode: String? = null,
)

@Serializable
data class SuggestedWordDto(
    val originalWord: String,
    val translation: String,
    val description: String = "",
)

@Serializable
data class SuggestWordsResponse(val items: List<SuggestedWordDto> = emptyList())
