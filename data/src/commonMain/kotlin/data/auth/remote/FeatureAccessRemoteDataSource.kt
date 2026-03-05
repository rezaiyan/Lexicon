package data.auth.remote

import data.core.network.client.ApiClient
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import core.common.fold
import expects.logNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val path = "/users/feature-access"
class FeatureAccessRemoteDataSource(private val apiClient: ApiClient) : IFeatureAccessRemoteDataSource {

    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> {
        return apiClient.getFlowNotNull<FeatureAccessResponse>(path)
            .map { result ->
                result.fold(
                    onSuccess = { featureAccess ->
                        logNetwork(
                            "FeatureAccessRemoteDataSource",
                            "Feature access retrieved=${featureAccess.userAccess.hasPremiumAccess}"
                        )
                        featureAccess
                    },
                    onFailure = { error ->
                        logNetwork("FeatureAccessRemoteDataSource", "Error getting feature access: ${error.message}")
                        defaultFeatureAccess()
                    }
                )
            }
    }

    private fun defaultFeatureAccess(): FeatureAccessResponse {
        return FeatureAccessResponse(
            featureFlags = FeatureFlags(pushNotificationsEnabled = true),
            userAccess = UserFeatureAccess(hasPremiumAccess = false)
        )
    }
}
