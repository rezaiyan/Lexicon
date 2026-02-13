package data.onboarding.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferencesRequest(
    val targetLanguage: String,
    val nativeLanguage: String,
    val currentLevel: String,
    val interests: List<String> = emptyList()
)

@Serializable
data class SuggestedVocabularyDto(
    val originalWord: String,
    val translation: String,
    val description: String
)

@Serializable
data class SuggestedVocabularyResponseDto(
    val targetLanguage: String,
    val nativeLanguage: String,
    val currentLevel: String,
    val items: List<SuggestedVocabularyDto>
)
