package domain.onboarding.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferences(
    val targetLanguage: String,
    val nativeLanguage: String,
    val level: String,
    val interests: List<String> = emptyList()
)
