package data.onboarding.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferencesRequest(
    val targetLanguage: String,
    val nativeLanguage: String,
    val level: String,
    val interests: List<String> = emptyList()
)

@Serializable
data class SuggestedVocabularyDto(
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String
)

@Serializable
data class SuggestedVocabularyResponseDto(
    val suggestedVocabulary: List<SuggestedVocabularyDto>,
    val collectionName: String,
    val totalCount: Int
)
