package data.onboarding.repository

import data.onboarding.remote.OnboardingRemoteDataSource
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.storage.SecureStorage
import domain.common.Try
import domain.common.fold
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class OnboardingRepositoryImpl(
    private val remoteDataSource: OnboardingRemoteDataSource,
    private val secureStorage: SecureStorage
) : IOnboardingRepository {

    // TODO: Remove fake response after testing
    private val useFakeResponse = true

    override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> {
        if (useFakeResponse) return Try.success(createFakeResponse(preferences))

        val request = OnboardingPreferencesRequest(
            targetLanguage = preferences.targetLanguage,
            nativeLanguage = preferences.nativeLanguage,
            currentLevel = preferences.level,
            interests = preferences.interests
        )
        return remoteDataSource.submitPreferences(request).fold(
            onSuccess = { dto ->
                val response = SuggestedVocabularyResponse(
                    suggestedVocabulary = dto.items.map { vocabDto ->
                        SuggestedVocabulary(
                            originalWord = vocabDto.originalWord,
                            translation = vocabDto.translation,
                            description = vocabDto.description,
                            sourceLanguage = dto.nativeLanguage,
                            targetLanguage = dto.targetLanguage
                        )
                    },
                    targetLanguage = dto.targetLanguage,
                    nativeLanguage = dto.nativeLanguage,
                    currentLevel = dto.currentLevel
                )
                Try.success(response)
            },
            onFailure = { error ->
                Try.failure(error)
            }
        )
    }

    private fun createFakeResponse(preferences: OnboardingPreferences) = SuggestedVocabularyResponse(
        targetLanguage = preferences.targetLanguage,
        nativeLanguage = preferences.nativeLanguage,
        currentLevel = preferences.level,
        suggestedVocabulary = listOf(
            SuggestedVocabulary("Hallo", "hello", "a greeting", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Guten Morgen", "good morning", "formal morning greeting", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Danke", "thank you", "expression of gratitude", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Bitte", "please / you're welcome", "polite request or response", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Ja", "yes", "affirmation", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Nein", "no", "negation", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Entschuldigung", "excuse me / sorry", "apology or getting attention", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Wasser", "water", "a drink", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Essen", "food / to eat", "noun or verb for eating", preferences.nativeLanguage, preferences.targetLanguage),
            SuggestedVocabulary("Freund", "friend", "a close person", preferences.nativeLanguage, preferences.targetLanguage),
        )
    )

    override suspend fun hasCompletedOnboarding(): Boolean {
        return secureStorage.hasCompletedOnboarding()
    }

    override suspend fun markOnboardingCompleted() {
        secureStorage.markOnboardingCompleted()
    }
}
