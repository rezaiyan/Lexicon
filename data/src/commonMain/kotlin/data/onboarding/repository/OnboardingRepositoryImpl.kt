package data.onboarding.repository

import data.onboarding.remote.OnboardingRemoteDataSource
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.storage.SecureStorage
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class OnboardingRepositoryImpl(
    private val remoteDataSource: OnboardingRemoteDataSource,
    private val secureStorage: SecureStorage
) : IOnboardingRepository {

    override suspend fun submitPreferences(preferences: OnboardingPreferences): Result<SuggestedVocabularyResponse> {
        val request = OnboardingPreferencesRequest(
            targetLanguage = preferences.targetLanguage,
            nativeLanguage = preferences.nativeLanguage,
            level = preferences.level,
            interests = preferences.interests
        )
        return remoteDataSource.submitPreferences(request).fold(
            onSuccess = { dto ->
                val response = SuggestedVocabularyResponse(
                    suggestedVocabulary = dto.suggestedVocabulary.map { vocabDto ->
                        SuggestedVocabulary(
                            originalWord = vocabDto.originalWord,
                            translation = vocabDto.translation,
                            description = vocabDto.description,
                            sourceLanguage = vocabDto.sourceLanguage,
                            targetLanguage = vocabDto.targetLanguage
                        )
                    },
                    collectionName = dto.collectionName,
                    totalCount = dto.totalCount
                )
                Result.success(response)
            },
            onFailure = { error ->
                Result.failure(error)
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
