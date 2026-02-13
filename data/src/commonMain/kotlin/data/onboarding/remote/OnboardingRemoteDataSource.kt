package data.onboarding.remote

import data.core.network.client.ApiClient
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyResponseDto
import domain.common.Try

class OnboardingRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun submitPreferences(request: OnboardingPreferencesRequest): Try<SuggestedVocabularyResponseDto> =
        apiClient.postNotNull("/onboarding/preferences", request)
}
