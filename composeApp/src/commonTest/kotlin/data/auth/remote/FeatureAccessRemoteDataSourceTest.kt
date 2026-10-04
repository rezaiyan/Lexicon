package data.auth.remote

import data.auth.local.IFeatureAccessLocalDataSource
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import fakes.FakeFeatureFlagProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import app.cash.turbine.test
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class FeatureAccessRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun buildApiClient(mockEngine: MockEngine): ApiClient {
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return ApiClient("https://api.test", httpClient, ApiResponseMapper())
    }

    private val featureFlagProvider = FakeFeatureFlagProvider()

    private val localCache = FakeFeatureAccessLocalDataSource()

    private fun buildDataSource(mockEngine: MockEngine, timeSource: TestTimeSource = TestTimeSource()) =
        FeatureAccessRemoteDataSource(
            buildApiClient(mockEngine), featureFlagProvider, localCache, timeSource, maxAge = 5.minutes
        )

    private fun successEnvelope(data: String) = """{"success":true,"data":$data}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `getFeatureAccessAsFlow emits feature access data on success`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope(
                    """{"featureFlags":{"pushNotificationsEnabled":false},"userAccess":{"hasPremiumAccess":true}}"""
                ),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertFalse(result.featureFlags.pushNotificationsEnabled)
        assertTrue(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `getFeatureAccessAsFlow sends GET to correct path`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                successEnvelope(
                    """{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""
                ),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertEquals("/users/feature-access", capturedPath)
    }

    @Test
    fun `getFeatureAccessAsFlow emits default feature access on HTTP error`() = runTest {
        val mockEngine = MockEngine {
            respond("Error", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertTrue(result.featureFlags.pushNotificationsEnabled)
        assertFalse(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `getFeatureAccessAsFlow emits default when API returns success false`() = runTest {
        val mockEngine = MockEngine {
            respond(
                """{"success":false,"data":null}""",
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertTrue(result.featureFlags.pushNotificationsEnabled)
        assertFalse(result.userAccess.hasPremiumAccess)
    }

    @Test
    fun `default feature access has pushNotificationsEnabled true and hasPremiumAccess false`() = runTest {
        val mockEngine = MockEngine {
            respond("Bad Gateway", HttpStatusCode.BadGateway, jsonHeaders())
        }
        val result = buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertEquals(true, result.featureFlags.pushNotificationsEnabled)
        assertEquals(false, result.userAccess.hasPremiumAccess)
    }

    // --- Cache behavior (BUG-3) ---

    @Test
    fun `getFeatureAccessAsFlow second call returns cached response without network request`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.getFeatureAccessAsFlow().first()  // cache miss — hits network
        dataSource.getFeatureAccessAsFlow().first()  // cache hit — no network

        assertEquals(1, requestCount)
    }

    @Test
    fun `clearCache forces re-fetch on next call`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":false}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.getFeatureAccessAsFlow().first()  // first fetch
        assertEquals(1, requestCount)

        dataSource.clearCache()

        dataSource.getFeatureAccessAsFlow().first()  // second fetch after cache cleared
        assertEquals(2, requestCount)
    }

    @Test
    fun `syncWithStore posts to sync path and pushes result to open collectors`() = runTest {
        var syncMethod: HttpMethod? = null
        var syncPath: String? = null
        val mockEngine = MockEngine { request ->
            val premium = request.url.encodedPath.endsWith("/subscriptions/sync")
            if (premium) {
                syncMethod = request.method
                syncPath = request.url.encodedPath
            }
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":$premium}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        dataSource.getFeatureAccessAsFlow().test {
            assertFalse(awaitItem().userAccess.hasPremiumAccess)

            val result = dataSource.syncWithStore()

            assertTrue(result.isSuccess)
            assertTrue(awaitItem().userAccess.hasPremiumAccess)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(HttpMethod.Post, syncMethod)
        assertEquals("/subscriptions/sync", syncPath)
    }

    @Test
    fun `syncWithStore failure keeps cached access`() = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/subscriptions/sync")) {
                respond("""{"success":false,"message":"Too many"}""", HttpStatusCode.TooManyRequests, jsonHeaders())
            } else {
                respond(
                    successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":true}}"""),
                    HttpStatusCode.OK,
                    jsonHeaders()
                )
            }
        }
        val dataSource = buildDataSource(mockEngine)
        dataSource.getFeatureAccessAsFlow().first()

        assertFalse(dataSource.syncWithStore().isSuccess)
        assertTrue(dataSource.getFeatureAccessAsFlow().first().userAccess.hasPremiumAccess)
    }

    @Test
    fun `cached response has same values as original fetch`() = runTest {
        val mockEngine = MockEngine {
            respond(
                successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":false},"userAccess":{"hasPremiumAccess":true}}"""),
                HttpStatusCode.OK,
                jsonHeaders()
            )
        }
        val dataSource = buildDataSource(mockEngine)

        val firstResult = dataSource.getFeatureAccessAsFlow().first()
        val cachedResult = dataSource.getFeatureAccessAsFlow().first()

        assertEquals(firstResult.featureFlags.pushNotificationsEnabled, cachedResult.featureFlags.pushNotificationsEnabled)
        assertEquals(firstResult.userAccess.hasPremiumAccess, cachedResult.userAccess.hasPremiumAccess)
    }

    private fun accessBody(premium: Boolean) =
        successEnvelope("""{"featureFlags":{"pushNotificationsEnabled":true},"userAccess":{"hasPremiumAccess":$premium}}""")

    @Test
    fun `refresh within max age skips the network`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(accessBody(premium = false), HttpStatusCode.OK, jsonHeaders())
        }
        val timeSource = TestTimeSource()
        val dataSource = buildDataSource(mockEngine, timeSource)
        dataSource.getFeatureAccessAsFlow().first()

        timeSource += 4.minutes
        assertTrue(dataSource.refresh().isSuccess)

        assertEquals(1, requestCount)
    }

    @Test
    fun `refresh after max age refetches and pushes change to open collectors`() = runTest {
        var premium = false
        val mockEngine = MockEngine { respond(accessBody(premium), HttpStatusCode.OK, jsonHeaders()) }
        val timeSource = TestTimeSource()
        val dataSource = buildDataSource(mockEngine, timeSource)

        dataSource.getFeatureAccessAsFlow().test {
            assertFalse(awaitItem().userAccess.hasPremiumAccess)

            premium = true
            timeSource += 6.minutes
            assertTrue(dataSource.refresh().isSuccess)

            assertTrue(awaitItem().userAccess.hasPremiumAccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure keeps cached access`() = runTest {
        var fail = false
        val mockEngine = MockEngine {
            if (fail) {
                respond("""{"success":false,"message":"down"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders())
            } else {
                respond(accessBody(premium = true), HttpStatusCode.OK, jsonHeaders())
            }
        }
        val timeSource = TestTimeSource()
        val dataSource = buildDataSource(mockEngine, timeSource)
        dataSource.getFeatureAccessAsFlow().first()

        fail = true
        timeSource += 6.minutes

        assertFalse(dataSource.refresh().isSuccess)
        assertTrue(dataSource.getFeatureAccessAsFlow().first().userAccess.hasPremiumAccess)
    }

    @Test
    fun `refresh with nothing cached fetches`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(accessBody(premium = true), HttpStatusCode.OK, jsonHeaders())
        }
        val dataSource = buildDataSource(mockEngine)

        assertTrue(dataSource.refresh().isSuccess)

        assertEquals(1, requestCount)
        assertTrue(dataSource.getFeatureAccessAsFlow().first().userAccess.hasPremiumAccess)
        assertEquals(1, requestCount)
    }

    // --- Device copy ---

    private fun savedAccess(premium: Boolean) = FeatureAccessResponse(
        featureFlags = FeatureFlags(pushNotificationsEnabled = true),
        userAccess = UserFeatureAccess(hasPremiumAccess = premium),
    )

    @Test
    fun `getFeatureAccessAsFlow emits device copy first then network result`() = runTest {
        localCache.saved = savedAccess(premium = true)
        val mockEngine = MockEngine { respond(accessBody(premium = false), HttpStatusCode.OK, jsonHeaders()) }

        buildDataSource(mockEngine).getFeatureAccessAsFlow().test {
            assertTrue(awaitItem().userAccess.hasPremiumAccess)
            assertFalse(awaitItem().userAccess.hasPremiumAccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFeatureAccessAsFlow offline with device copy keeps premium instead of downgrading`() = runTest {
        localCache.saved = savedAccess(premium = true)
        val mockEngine = MockEngine { respond("down", HttpStatusCode.ServiceUnavailable, jsonHeaders()) }

        buildDataSource(mockEngine).getFeatureAccessAsFlow().test {
            assertTrue(awaitItem().userAccess.hasPremiumAccess)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful fetch is written to the device copy`() = runTest {
        val mockEngine = MockEngine { respond(accessBody(premium = true), HttpStatusCode.OK, jsonHeaders()) }

        buildDataSource(mockEngine).getFeatureAccessAsFlow().first()

        assertEquals(true, localCache.saved?.userAccess?.hasPremiumAccess)
    }

    @Test
    fun `device copy is not treated as fresh so refresh hits the network`() = runTest {
        localCache.saved = savedAccess(premium = true)
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond("down", HttpStatusCode.ServiceUnavailable, jsonHeaders())
        }
        val dataSource = buildDataSource(mockEngine)
        // first() takes the device copy and cancels before the flow's own fetch
        dataSource.getFeatureAccessAsFlow().first()

        dataSource.refresh()

        assertEquals(1, requestCount)
        assertTrue(dataSource.getFeatureAccessAsFlow().first().userAccess.hasPremiumAccess)
    }

    @Test
    fun `forced refresh within max age still hits the network`() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine {
            requestCount++
            respond(accessBody(premium = false), HttpStatusCode.OK, jsonHeaders())
        }
        val dataSource = buildDataSource(mockEngine)
        dataSource.getFeatureAccessAsFlow().first()

        assertTrue(dataSource.refresh(force = true).isSuccess)

        assertEquals(2, requestCount)
    }

    @Test
    fun `clearCache wipes the device copy`() = runTest {
        localCache.saved = savedAccess(premium = true)
        val dataSource = buildDataSource(MockEngine { respond(accessBody(true), HttpStatusCode.OK, jsonHeaders()) })

        dataSource.clearCache()

        assertNull(localCache.saved)
    }
}

private class FakeFeatureAccessLocalDataSource : IFeatureAccessLocalDataSource {
    var saved: FeatureAccessResponse? = null

    override suspend fun read(): FeatureAccessResponse? = saved

    override suspend fun write(featureAccess: FeatureAccessResponse) {
        saved = featureAccess
    }

    override suspend fun clear() {
        saved = null
    }
}
