package data.onboarding.repository

import core.common.Try
import core.common.map
import data.onboarding.remote.IOnboardingRemoteDataSource
import data.onboarding.remote.model.toDomain
import data.onboarding.remote.model.toRequest
import data.storage.SecureStorage
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository

class OnboardingRepositoryImpl(
    private val remoteDataSource: IOnboardingRemoteDataSource,
    private val secureStorage: SecureStorage
) : IOnboardingRepository {

    override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<List<SuggestedVocabulary>> =
        remoteDataSource.submitPreferences(preferences.toRequest())
            .map { dto -> dto.items.map { it.toDomain(preferences) } }

    override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try {
        secureStorage.hasCompletedOnboarding()
    }

    override suspend fun markOnboardingCompleted(): Try<Unit> = Try {
        secureStorage.markOnboardingCompleted()
    }

    override suspend fun resetOnboarding(): Try<Unit> = Try {
        secureStorage.clearOnboardingCompleted()
    }
}
