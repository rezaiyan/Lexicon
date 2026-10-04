package data.auth.remote

import core.common.Try
import core.common.onFailure
import core.common.map
import core.common.onSuccess
import data.core.network.client.ApiClient
import data.auth.local.IFeatureAccessLocalDataSource
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val FEATURE_ACCESS_PATH = "/users/feature-access"
private const val SYNC_PATH = "/subscriptions/sync"
private const val TAG = "FeatureAccessRemoteDataSource"

/**
 * Feature access with a process-wide cache, backed by a device copy ([localCache]).
 *
 * Collectors stay subscribed to the cache, so a [syncWithStore] or [refresh] result (e.g. right
 * after a purchase, or a subscription change pushed by the server) reaches every open screen
 * immediately. On start the device copy is shown at once and then refreshed from the network; a
 * failed request keeps whatever is known, so a flaky network never downgrades a paying user.
 */
class FeatureAccessRemoteDataSource(
    private val apiClient: ApiClient,
    private val featureFlagProvider: IFeatureFlagProvider,
    private val localCache: IFeatureAccessLocalDataSource,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val maxAge: Duration = 5.minutes,
) : IFeatureAccessRemoteDataSource {

    private val cache = MutableStateFlow<FeatureAccessResponse?>(null)
    private var fetchedAt: TimeMark? = null

    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flow {
        if (cache.value == null) {
            // Device copy first: instant and offline-safe. Not marked fresh, so it's revalidated below.
            localCache.read()?.let { saved ->
                cache.compareAndSet(null, saved)
                emit(saved)
            }
            fetch()
                .onSuccess { logNetwork(TAG, "Feature access retrieved=${it.userAccess.hasPremiumAccess}") }
                .onFailure { error ->
                    logNetwork(TAG, "Error getting feature access: ${error.message}")
                    if (cache.value == null) emit(defaultFeatureAccess())
                }
        }
        emitAll(cache.filterNotNull())
    }.distinctUntilChanged()

    override suspend fun syncWithStore(): Try<FeatureAccessResponse> =
        apiClient.postNotNull<FeatureAccessResponse>(SYNC_PATH)
            .onSuccess { store(it) }

    override suspend fun refresh(force: Boolean): Try<Unit> {
        val isFresh = fetchedAt?.let { it.elapsedNow() < maxAge } == true
        if (isFresh && !force) return Try.success(Unit)
        return fetch().map { }
    }

    override suspend fun clearCache() {
        cache.value = null
        fetchedAt = null
        localCache.clear()
    }

    private suspend fun fetch(): Try<FeatureAccessResponse> =
        apiClient.getNotNull<FeatureAccessResponse>(FEATURE_ACCESS_PATH)
            .onSuccess { store(it) }

    private suspend fun store(featureAccess: FeatureAccessResponse) {
        cache.value = featureAccess
        fetchedAt = timeSource.markNow()
        localCache.write(featureAccess)
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
