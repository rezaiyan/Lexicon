package data.onboarding.remote

import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyResponseDto
import core.common.Try

interface IOnboardingRemoteDataSource {
    suspend fun submitPreferences(request: OnboardingPreferencesRequest): Try<SuggestedVocabularyResponseDto>
}
