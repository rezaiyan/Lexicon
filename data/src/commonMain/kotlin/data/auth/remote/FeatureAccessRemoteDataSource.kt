package data.auth.remote

import core.common.Try
import core.common.fold
import core.common.map
import core.common.onSuccess
import data.core.network.client.ApiClient
import data.core.network.client.getFlowNotNull
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.featureflag.IFeatureFlagProvider
import expects.logNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val FEATURE_ACCESS_PATH = "/users/feature-access"
private const val SYNC_PATH = "/subscriptions/sync"

/**
 * Feature access with a process-wide cache. Collectors stay subscribed to the cache, so a
 * [syncWithStore] or [refresh] result (e.g. right after a purchase, or a grant/expiry that
 * happened while the app was in the background) reaches every open screen immediately.
 */
class FeatureAccessRemoteDataSource(
    private val apiClient: ApiClient,
    private val featureFlagProvider: IFeatureFlagProvider,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val maxAge: Duration = 5.minutes,
) : IFeatureAccessRemoteDataSource {

    private val cache = MutableStateFlow<FeatureAccessResponse?>(null)
    private var fetchedAt: TimeMark? = null

    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flow {
        if (cache.value == null) emitAll(fetch())
        emitAll(cache.filterNotNull())
    }.distinctUntilChanged()

    private fun fetch(): Flow<FeatureAccessResponse> =
        apiClient.getFlowNotNull<FeatureAccessResponse>(FEATURE_ACCESS_PATH)
            .map { result ->
                result.fold(
                    onSuccess = { featureAccess ->
                        store(featureAccess)
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

    override suspend fun syncWithStore(): Try<FeatureAccessResponse> =
        apiClient.postNotNull<FeatureAccessResponse>(SYNC_PATH)
            .onSuccess(::store)

    override suspend fun refresh(): Try<Unit> {
        val isFresh = fetchedAt?.let { it.elapsedNow() < maxAge } == true
        if (isFresh) return Try.success(Unit)
        return apiClient.getNotNull<FeatureAccessResponse>(FEATURE_ACCESS_PATH)
            .onSuccess(::store)
            .map { }
    }

    override fun clearCache() {
        cache.value = null
        fetchedAt = null
    }

    private fun store(featureAccess: FeatureAccessResponse) {
        cache.value = featureAccess
        fetchedAt = timeSource.markNow()
    }

    private fun defaultFeatureAccess(): FeatureAccessResponse {
        return FeatureAccessResponse(
            featureFlags = FeatureFlags(
                pushNotificationsEnabled = featureFlagProvider.getBoolean(
                    "push_notifications_enabled",
                    default = true
                )
            ),
            userAccess = UserFeatureAccess(hasPremiumAccess = false)
        )
    }
}
