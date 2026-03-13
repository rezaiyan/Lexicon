package data.onboarding.repository

import data.onboarding.remote.IOnboardingRemoteDataSource
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.storage.SecureStorage
import core.common.Try
import core.common.fold
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class OnboardingRepositoryImpl(
    private val remoteDataSource: IOnboardingRemoteDataSource,
    private val secureStorage: SecureStorage
) : IOnboardingRepository {

    override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> {
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

    override suspend fun hasCompletedOnboarding(): Boolean {
        return secureStorage.hasCompletedOnboarding()
    }

    override suspend fun markOnboardingCompleted() {
        secureStorage.markOnboardingCompleted()
    }
}
