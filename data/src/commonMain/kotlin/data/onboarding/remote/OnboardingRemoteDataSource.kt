package data.onboarding.remote

import data.core.network.client.ApiClient
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyResponseDto
import core.common.Try

class OnboardingRemoteDataSource(
    private val apiClient: ApiClient
) : IOnboardingRemoteDataSource {
    override suspend fun submitPreferences(request: OnboardingPreferencesRequest): Try<SuggestedVocabularyResponseDto> =
        apiClient.postNotNull("/onboarding/preferences", request)
}
