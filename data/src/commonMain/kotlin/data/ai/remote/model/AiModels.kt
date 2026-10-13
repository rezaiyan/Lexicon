package data.ai.remote.model

import domain.ai.model.VocabularyFocus
import kotlinx.serialization.Serializable

/** v2 photo extraction (`POST /ai/extract-words`): explicit language pair, structured items. */
@Serializable
data class ExtractWordsRequest(
    val imageBase64: String,
    val learningLanguage: String,
    val nativeLanguage: String,
    val includePhrases: Boolean = false,
    /** Quick picks; free. */
    val focus: VocabularyFocusDto? = null,
    /** The learner's own words; screened server-side and charged as a guided extraction. */
    val instructions: String? = null,
)

/** Quick picks, as the server's `VocabularyFocus`: enum names are part of the contract. */
@Serializable
data class VocabularyFocusDto(
    val wordTypes: Set<String> = emptySet(),
    val register: String? = null,
    val noteStyle: String? = null,
    val markedOnly: Boolean = false,
    val skipNames: Boolean = false,
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
    /** Quick picks; free. */
    val focus: VocabularyFocusDto? = null,
    /** The learner's own words; screened server-side and charged as guided suggestions. */
    val instructions: String? = null,
)

@Serializable
data class SuggestedWordDto(
    val originalWord: String,
    val translation: String,
    val description: String = "",
)

@Serializable
data class SuggestWordsResponse(val items: List<SuggestedWordDto> = emptyList())

/** Null when nothing is picked, so plain requests look exactly as before. */
fun VocabularyFocus.toDtoOrNull(): VocabularyFocusDto? = takeUnless { it.isEmpty }?.let {
    VocabularyFocusDto(
        wordTypes = wordTypes.mapTo(mutableSetOf()) { type -> type.name },
        register = register?.name,
        noteStyle = noteStyle?.name,
        markedOnly = markedOnly,
        skipNames = skipNames,
    )
}
