package domain.onboarding.model

import kotlinx.serialization.Serializable

@Serializable
data class SuggestedVocabulary(
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String
)

@Serializable
data class SuggestedVocabularyResponse(
    val suggestedVocabulary: List<SuggestedVocabulary>,
    val targetLanguage: String,
    val nativeLanguage: String,
    val currentLevel: String
)
