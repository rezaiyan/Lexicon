package data.onboarding.remote

import data.core.network.client.ApiClient
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyResponseDto

class OnboardingRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun submitPreferences(request: OnboardingPreferencesRequest): Result<SuggestedVocabularyResponseDto> =
        apiClient.postNotNull("/onboarding/preferences", request)
}
