package data.onboarding.remote.model

import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.ProficiencyLevel
import domain.onboarding.model.SuggestedVocabulary

/** The backend takes languages by display name ("German") and levels as lowercase keys ("beginner"). */
fun OnboardingPreferences.toRequest(): OnboardingPreferencesRequest = OnboardingPreferencesRequest(
    targetLanguage = targetLanguage.displayName,
    nativeLanguage = nativeLanguage.displayName,
    currentLevel = level.toApiValue(),
    interests = interests,
)

fun ProficiencyLevel.toApiValue(): String = name.lowercase()

/** Words come back in the requested pair, so the request's languages label them. */
fun SuggestedVocabularyDto.toDomain(preferences: OnboardingPreferences): SuggestedVocabulary = SuggestedVocabulary(
    originalWord = originalWord,
    translation = translation,
    description = description,
    sourceLanguage = preferences.nativeLanguage,
    targetLanguage = preferences.targetLanguage,
)
